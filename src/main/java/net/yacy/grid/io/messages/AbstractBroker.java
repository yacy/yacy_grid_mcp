/**
 *  AbstractBroker
 *  Copyright 08.07.2017 by Michael Peter Christen, @orbiterlab
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
import net.yacy.grid.YaCyServices;
import net.yacy.grid.tools.Logger;

public abstract class AbstractBroker implements Broker {

    private final static Random random = new Random();
    private final Map<Services, AtomicInteger> roundRobinLookup = new ConcurrentHashMap<>();
    private final Map<Services, Map<String, Integer>> leastFilledLookup = new ConcurrentHashMap<>();
    private final Map<Services, Set<String>> switchedIDsMap = new ConcurrentHashMap<>();

    @Override
    public abstract void close() throws IOException;

    @Override
    public abstract QueueFactory send(final Services service, final GridQueue queue, final byte[] message) throws IOException;

    @Override
    public QueueFactory send(final Services service, final GridQueue[] queues, final ShardingMethod shardingMethod, final int[] priorityDimensions, final int priority, final String hashingKey, final byte[] message) throws IOException {
        return send(service, queueName(service, queues, shardingMethod, priorityDimensions, priority, hashingKey), message);
    }

    @Override
    public GridQueue queueName(final Services service, final GridQueue[] queues, final ShardingMethod shardingMethod, final int[] priorityDimensions, final int priority, final String hashingKey) throws IOException {
        if (queues.length == 1) return queues[0];
        assert priorityDimensions.length > priority;
        int queuesBeforeCurrentDimension = 0;
        for (int i = 0; i < priority; i++) queuesBeforeCurrentDimension += priorityDimensions[i];
        final int priorityDimension = priorityDimensions[priority];
        final GridQueue[] psq = new GridQueue[priorityDimension];
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
    public abstract MessageContainer receive(final Services service, final GridQueue queue, long timeout, boolean autoAck) throws IOException;

    @Override
    public abstract AvailableContainer available(final Services service, final GridQueue queue) throws IOException;

    private final Map<String, AvailableContainer> acbuffers = new ConcurrentHashMap<>();
    private final Map<String, Long> actime = new ConcurrentHashMap<>();
    public AvailableContainer bufferedAvailable(final Services service, final GridQueue queue) throws IOException {
        final String bkey = service.name() + "_" + queue.name();
        final Long lacc = this.actime.get(bkey);
        final long laccl = lacc == null ? 0 : lacc.longValue();
        final long now = System.currentTimeMillis();
        AvailableContainer ac = this.acbuffers.get(bkey);
        if (ac == null || now - laccl > 10000) {
            ac = available(service, queue);
            this.acbuffers.put(bkey, ac);
            this.actime.put(bkey, now);
        }
        return ac;
    }

    @Override
    public AvailableContainer[] available(final Services service, final GridQueue[] queues) throws IOException {
        final AvailableContainer[] ac = new AvailableContainer[queues.length];
        for (int i = 0; i < queues.length; i++) {
            ac[i] = bufferedAvailable(service, queues[i]);
        }
        return ac;
    }

    @Override
    public List<MessageContainer> peek(final YaCyServices service, final GridQueue queue, int count) {
        final List<MessageContainer> messages = new ArrayList<>();
        fetch: while (count-- > 0) {
            MessageContainer message = null;
            try {
                message = receive(service, queue, 3000, true); // if there are no more messages, this simply runs into a time-out
            } catch (final IOException e) {
                break fetch;
            }

            // message can be null if a timeout occurred or no more messages are in the queue
            if (message == null) break fetch;
            messages.add(message);
        }
        // send messages again to queue asap!
        for (int i = messages.size() - 1; i >= 0; i--) {
            try {
                send(service, queue, messages.get(i).getPayload());
            } catch (final IOException e) {
                // this is bad
                e.printStackTrace();
            }
        }
        return messages;
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
    private int leastFilled(final AvailableContainer[] ac) throws IOException {
        if (ac.length == 1) return 0;
        int index = random.nextInt(ac.length);
        long leastAvailable = Long.MAX_VALUE;
        final List<Integer> zeroCandidates = new ArrayList<>();
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
            final AvailableContainer[] available = available(service, queues);
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
        final AvailableContainer[] available = available(service, queues);
        // because this available object comes from a buffered object which may be outdated right now already it is important to pick random elements out of it!
        assert available.length == queues.length;
        final int leastFilled = leastFilled(available);
        assert leastFilled < queues.length;
        if (lookupIndex == null) {
            // find a new queue with least entries
            lookupIndex = leastFilled;
            lookupMap.put(hashingKey, lookupIndex);
        } else {
            // Check if this hashing key was never switched to a different queue
            // and if an empty queue exist: then switch to that queue to balance all queues.
            // That means also that every domain may only switched once
            Set<String> switchedIDs = this.switchedIDsMap.get(service);
            if (switchedIDs == null) {
                switchedIDs = ConcurrentHashMap.newKeySet();
                this.switchedIDsMap.put(service, switchedIDs);
            }
            if (available[lookupIndex].getAvailable() > 100 && available[leastFilled].getAvailable() == 0 && !switchedIDs.contains(hashingKey)) {
                switchedIDs.add(hashingKey);
                // switch to leastFilled
                Logger.info(this.getClass(), "AbstractBroker switching " + hashingKey + " from " + lookupIndex + " to " + leastFilled);
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
