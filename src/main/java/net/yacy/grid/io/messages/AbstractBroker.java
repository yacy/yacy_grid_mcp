/**
 *  AbstractBroker
 *  Copyright 08.07.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.io.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.yacy.grid.Services;
import net.yacy.grid.mcp.Data;

public abstract class AbstractBroker<A> implements Broker<A> {

    private final static Random random = new Random();
    private final Map<Services, AtomicInteger> roundRobinLookup = new ConcurrentHashMap<>();
    private final Map<Services, Map<String, Integer>> leastFilledLookup = new ConcurrentHashMap<>();
    private final Map<Services, Set<String>> switchedIDsMap = new ConcurrentHashMap<>();
    
    @Override
    public abstract void close() throws IOException;

    @Override
    public abstract QueueFactory<A> send(final Services service, final GridQueue queue, final byte[] message) throws IOException;

    @Override
    public QueueFactory<A> send(final Services service, final GridQueue[] queues, final ShardingMethod shardingMethod, int[] priorityDimensions, int priority, final String hashingKey, final byte[] message) throws IOException {
        return send(service, queueName(service, queues, shardingMethod, priorityDimensions, priority, hashingKey), message);
    }

    @Override
    public GridQueue queueName(final Services service, final GridQueue[] queues, final ShardingMethod shardingMethod, int[] priorityDimensions, int priority, final String hashingKey) throws IOException {
        if (queues.length == 1) return queues[0];
        assert priorityDimensions.length > priority;
        int queuesBeforeCurrentDimension = 0;
        for (int i = 0; i < priority; i++) queuesBeforeCurrentDimension += priorityDimensions[i];
        int priorityDimension = priorityDimensions[priority];
        GridQueue[] psq = new GridQueue[priorityDimension];
        System.arraycopy(queues, queuesBeforeCurrentDimension, psq, 0, priorityDimension);
        int idx = 0;
        switch (shardingMethod) {
            case ROUND_ROBIN:
                idx = roundRobin(service, psq);
                break;
            case LEAST_FILLED:
                idx = leastFilled(available(service, psq));
                break;
            case HASH:
                idx = hash(service, psq, hashingKey);
                break;
            case LOOKUP:
                idx = lookup(service, psq, hashingKey);
                break;
            case BALANCE:
                idx = balance(service, psq, hashingKey);
                break;
            case RANDOM:
                idx = random(service, psq);
                break;
            case FIRST:
                idx = first(service, psq);
                break;
            default:
                idx = first(service, psq);
                break;
        }
        assert idx < psq.length;
        if (idx >= psq.length) idx = 0;
        return psq[idx];
    }
    

    @Override
    public abstract MessageContainer<A> receive(final Services service, final GridQueue queue, long timeout, boolean autoAck) throws IOException;

    @Override
    public abstract AvailableContainer available(final Services service, final GridQueue queue) throws IOException;

    private Map<String, AvailableContainer> acbuffers = new ConcurrentHashMap<>();
    private Map<String, Long> actime = new ConcurrentHashMap<>();
    public AvailableContainer bufferedAvailable(final Services service, final GridQueue queue) throws IOException {
        String bkey = service.name() + "_" + queue.name();
        Long lacc = actime.get(bkey);
        long laccl = lacc == null ? 0 : lacc.longValue();
        long now = System.currentTimeMillis();
        AvailableContainer ac = acbuffers.get(bkey);
        if (ac == null || now - laccl > 10000) {
            ac = available(service, queue);
            acbuffers.put(bkey, ac);
            actime.put(bkey, now);
        }
        return ac;
    }
    
    @Override
    public AvailableContainer[] available(final Services service, final GridQueue[] queues) throws IOException {
        AvailableContainer[] ac = new AvailableContainer[queues.length];
        for (int i = 0; i < queues.length; i++) {
            ac[i] = bufferedAvailable(service, queues[i]);
        }
        return ac;
    }

    private int roundRobin(final Services service, final GridQueue[] queues) throws IOException {
        AtomicInteger latestCounter = this.roundRobinLookup.get(service);
        if (latestCounter == null) {
            latestCounter = new AtomicInteger(0);
            this.roundRobinLookup.put(service, latestCounter);
            return 0;
        }
        if (latestCounter.incrementAndGet() >= queues.length) latestCounter.set(0);
        return latestCounter.get();
    }
    
    /**
     * pick one container out of the given one which has the least number of entries.
     * Because the input container may be outdated right now (it comes from a buffer)
     * it is important to pick random elements out of it.
     * @param ac
     * @return
     * @throws IOException
     */
    private int leastFilled(AvailableContainer[] ac) throws IOException {
        if (ac.length == 1) return 0;
        int index = random.nextInt(ac.length);
        long leastAvailable = Long.MAX_VALUE;
        List<Integer> zeroCandidates = new ArrayList<>();
        for (int i = 0; i < ac.length; i++) {
            if (ac[i].getAvailable() == 0) zeroCandidates.add(i);
            if (ac[i].getAvailable() < leastAvailable) {
                leastAvailable = ac[i].getAvailable();
                index = i;
            }
        }
        if (zeroCandidates.size() > 0) {
            	return zeroCandidates.get(random.nextInt(zeroCandidates.size()));
        }
        return index;
    }
    
    private int hash(final Services service, final GridQueue[] queues, final String hashingKey) throws IOException {
        return hashingKey.hashCode() % queues.length;
    }
    
    private int lookup(final Services service, final GridQueue[] queues, final String hashingKey) throws IOException {
        if (queues.length == 1) return 0;
        Map<String, Integer> lookupMap = this.leastFilledLookup.get(service);
        if (lookupMap == null) {
            lookupMap = new ConcurrentHashMap<>();
            this.leastFilledLookup.put(service, lookupMap);
        }
        Integer lookupIndex = lookupMap.get(hashingKey);
        if (lookupIndex == null) {
            AvailableContainer[] available = available(service, queues);
            lookupIndex = leastFilled(available);
            lookupMap.put(hashingKey, lookupIndex);
        }
        return lookupIndex;
    }
    
    private int balance(final Services service, final GridQueue[] queues, final String hashingKey) throws IOException {
        if (queues.length == 1) return 0;
        Map<String, Integer> lookupMap = this.leastFilledLookup.get(service);
        if (lookupMap == null) {
            lookupMap = new ConcurrentHashMap<>();
            this.leastFilledLookup.put(service, lookupMap);
        }
        Integer lookupIndex = lookupMap.get(hashingKey);
        AvailableContainer[] available = available(service, queues);
        // because this available object comes from a buffered object which may be outdated right now already it is important to pick random elements out of it!
        assert available.length == queues.length;
        int leastFilled = leastFilled(available);
        assert leastFilled < queues.length;
        if (lookupIndex == null) {
            // find a new queue with least entries
            lookupIndex = leastFilled;
            lookupMap.put(hashingKey, lookupIndex);
        } else {
            // Check if this hashing key was never switched to a different queue
            // and if an empty queue exist: then switch to that queue to balance all queues.
            // That means also that every domain may only switched once
            Set<String> switchedIDs = switchedIDsMap.get(service);
            if (switchedIDs == null) {
                switchedIDs = ConcurrentHashMap.newKeySet();
                switchedIDsMap.put(service, switchedIDs);
            }
            if (available[lookupIndex].getAvailable() > 100 && available[leastFilled].getAvailable() == 0 && !switchedIDs.contains(hashingKey)) {
                switchedIDs.add(hashingKey);
                // switch to leastFilled
                Data.logger.info("AbstractBroker switching " + hashingKey + " from " + lookupIndex + " to " + leastFilled);
                lookupIndex = leastFilled;
                lookupMap.put(hashingKey, lookupIndex);
            }
        }
        assert lookupIndex < queues.length;
        return lookupIndex;
    }
    
    private int random(final Services service, final GridQueue[] queues) throws IOException {
        return random.nextInt(queues.length);
    }
    
    private int first(final Services service, final GridQueue[] queues) throws IOException {
        return 0;
    }
    
}
