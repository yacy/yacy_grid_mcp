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

public abstract class AbstractBroker<A> implements Broker<A> {

    private final static Random random = new Random();
    private final Map<Services, AtomicInteger> roundRobinLookup = new ConcurrentHashMap<>();
    private final Map<Services, Integer> leastFilledLookup = new ConcurrentHashMap<>();
    
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
        switch (shardingMethod) {
            case ROUND_ROBIN:
                return queueNames[roundRobin(service, queueNames)];
            case LEAST_FILLED:
                return queueNames[leastFilled(service, queueNames)];
            case HASH:
                return queueNames[hash(service, queueNames, hashingKey)];
            case LOOKUP:
                return queueNames[lookup(service, queueNames, hashingKey)];
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

    @Override
    public AvailableContainer[] available(final Services service, final QueueName[] queueNames) throws IOException {
        AvailableContainer[] ac = new AvailableContainer[queueNames.length];
        for (int i = 0; i < queueNames.length; i++) {
            ac[i] = available(service, queueNames[i]);
        }
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
    
    private int leastFilled(final Services service, final QueueName[] queueNames) throws IOException {
        if (queueNames.length == 1) return 0;
        AvailableContainer[] ac = available(service, queueNames);
        int index = -1;
        int leastAvailable = Integer.MAX_VALUE;
        for (int i = 0; i < ac.length; i++) {
            if (ac[i].getAvailable() < leastAvailable) { leastAvailable = ac[i].getAvailable(); index = i; }
        }
        return index;
    }
    
    private int hash(final Services service, final QueueName[] queueNames, final String hashingKey) throws IOException {
        return hashingKey.hashCode() % queueNames.length;
    }
    
    private int lookup(final Services service, final QueueName[] queueNames, final String hashingKey) throws IOException {
        Integer lookupIndex = this.leastFilledLookup.get(service);
        if (lookupIndex == null) {
            lookupIndex = leastFilled(service, queueNames);
            this.leastFilledLookup.put(service, lookupIndex);
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
