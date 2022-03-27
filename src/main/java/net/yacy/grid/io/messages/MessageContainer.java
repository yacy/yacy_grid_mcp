/**
 *  MessageContainer
 *  Copyright 28.1.2017 by Michael Peter Christen, @orbiterlab
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

public class MessageContainer {

    private final QueueFactory factory;
    private final byte[] payload;
    private final long deliveryTag;

    public MessageContainer(final QueueFactory factory, final byte[] payload, final long deliveryTag) {
        this.factory = factory;
        this.payload = payload;
        this.deliveryTag = deliveryTag;
    }

    public QueueFactory getFactory() {
        return this.factory;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public long getDeliveryTag() {
        return this.deliveryTag;
    }
}
