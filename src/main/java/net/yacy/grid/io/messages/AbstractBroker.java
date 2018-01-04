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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.yacy.grid.QueueName;
import net.yacy.grid.Services;
import net.yacy.grid.mcp.Data;

public abstract class AbstractBroker<A> implements Broker<A> {

    private final static Random random = new Random();
    private final Map<Services, AtomicInteger> roundRobinLookup = new ConcurrentHashMap<>();
    private final Map<Services, Map<String, Integer>> leastFilledLookup = new ConcurrentHashMap<>();
    
    @Override
    public abstract void close() throws IOException;

    @Override
    public abstract QueueFactory<A> send(final Services service, final QueueName queueName, final byte[] message) throws IOException;

    @Override
    public QueueFactory<A> send(final Services service, final QueueName[] queueNames, final ShardingMethod shardingMethod, final String hashingKey, final byte[] message) throws IOException {
        return send(service, queueName(service, queueNames, shardingMethod, hashingKey), message);
    }

    @Override
    public QueueName queueName(final Services service, final QueueName[] queueNames, final ShardingMethod shardingMethod, final String hashingKey) throws IOException {
        if (queueNames.length == 1) return queueNames[0];
        switch (shardingMethod) {
            case ROUND_ROBIN:
                return queueNames[roundRobin(service, queueNames)];
            case LEAST_FILLED:
                return queueNames[leastFilled(available(service, queueNames))];
            case HASH:
                return queueNames[hash(service, queueNames, hashingKey)];
            case LOOKUP:
                return queueNames[lookup(service, queueNames, hashingKey)];
            case BALANCE:
                return queueNames[balance(service, queueNames, hashingKey)];
            case RANDOM:
                return queueNames[random(service, queueNames)];
            case FIRST:
                return queueNames[first(service, queueNames)];
            default:
                return queueNames[first(service, queueNames)];
        }
    }

    @Override
    public abstract MessageContainer<A> receive(final Services service, final QueueName queueName, long timeout) throws IOException;

    @Override
    public abstract AvailableContainer available(final Services service, final QueueName queueName) throws IOException;
    
    private AvailableContainer[] acbuffer = null;
    private long actime = 0;
    
    @Override
    public AvailableContainer[] available(final Services service, final QueueName[] queueNames) throws IOException {
        long now = System.currentTimeMillis();
        if (acbuffer != null && now - actime < 10000) return acbuffer;
        
        AvailableContainer[] ac = new AvailableContainer[queueNames.length];
        for (int i = 0; i < queueNames.length; i++) {
            ac[i] = available(service, queueNames[i]);
        }
        acbuffer = ac;
        actime = now;
        return ac;
    }

    private int roundRobin(final Services service, final QueueName[] queueNames) throws IOException {
        AtomicInteger latestCounter = this.roundRobinLookup.get(service);
        if (latestCounter == null) {
            latestCounter = new AtomicInteger(0);
            this.roundRobinLookup.put(service, latestCounter);
            return 0;
        }
        if (latestCounter.incrementAndGet() >= queueNames.length) latestCounter.set(0);
        return latestCounter.get();
    }
    
    private int leastFilled(AvailableContainer[] ac) throws IOException {
        int index = -1;
        int leastAvailable = Integer.MAX_VALUE;
        for (int i = 0; i < ac.length; i++) {
            if (ac[i].getAvailable() < leastAvailable) {
                leastAvailable = ac[i].getAvailable();
                index = i;
                if (leastAvailable == 0) return index; // it does not get smaller 
            }
        }
        return index;
    }
    
    private int mostFilled(AvailableContainer[] ac) throws IOException {
        int index = -1;
        int mostAvailable = 0;
        for (int i = 0; i < ac.length; i++) {
            if (ac[i].getAvailable() > mostAvailable) {
                mostAvailable = ac[i].getAvailable();
                index = i;
            }
        }
        return index;
    }
    
    private int hash(final Services service, final QueueName[] queueNames, final String hashingKey) throws IOException {
        return hashingKey.hashCode() % queueNames.length;
    }
    
    private int lookup(final Services service, final QueueName[] queueNames, final String hashingKey) throws IOException {
        if (queueNames.length == 1) return 0;
        Map<String, Integer> lookupMap = this.leastFilledLookup.get(service);
        if (lookupMap == null) {
            lookupMap = new ConcurrentHashMap<>();
            this.leastFilledLookup.put(service, lookupMap);
        }
        Integer lookupIndex = lookupMap.get(hashingKey);
        if (lookupIndex == null) {
            AvailableContainer[] available = available(service, queueNames);
            lookupIndex = leastFilled(available);
            lookupMap.put(hashingKey, lookupIndex);
        }
        return lookupIndex;
    }
    
    private int balance(final Services service, final QueueName[] queueNames, final String hashingKey) throws IOException {
        if (queueNames.length == 1) return 0;
        Map<String, Integer> lookupMap = this.leastFilledLookup.get(service);
        if (lookupMap == null) {
            lookupMap = new ConcurrentHashMap<>();
            this.leastFilledLookup.put(service, lookupMap);
        }
        Integer lookupIndex = lookupMap.get(hashingKey);
        AvailableContainer[] available = available(service, queueNames);
        int leastFilled = leastFilled(available);
        if (lookupIndex == null) {
            // find a new queue with least entries
            lookupIndex = leastFilled;
            lookupMap.put(hashingKey, lookupIndex);
        } else {
            // check if this index is identical with the one with most entries
            // and if an empty queue exist
            if (available[leastFilled].getAvailable() == 0 && mostFilled(available) == lookupIndex.intValue()) {
                // switch to leastFilled
                Data.logger.info("AbstractBroker switching " + hashingKey + " from " + lookupIndex + " to " + leastFilled);
                lookupIndex = leastFilled;
                lookupMap.put(hashingKey, lookupIndex);
            }
        }
        return lookupIndex;
    }
    
    private int random(final Services service, final QueueName[] queueNames) throws IOException {
        return random.nextInt(queueNames.length);
    }
    
    private int first(final Services service, final QueueName[] queueNames) throws IOException {
        return 0;
    }
    
}
