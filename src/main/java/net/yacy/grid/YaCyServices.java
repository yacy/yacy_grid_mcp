/**
 *  Services
 *  Copyright 16.01.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid;

/**
 * Service name declaration with default port numbers.
 * These port numbers are just recommendations for standard ports.
 * As the YaCy grid is supposed to scale with the number of services attached,
 * more services must be able to acquire much more port numbers as given here.
 * We consider that alternative/additional port numbers are successive numbers
 * starting with the default numbers as given here.
 * 
 * Non-YaCy services may be able to connect to the mcp as well. These services
 * may consider other data types different from web documents as input for
 * an indexing grid. Therefore we prefer the prefix 'yacy' for JSON documents
 * using the YaCy object schema. Other object schema types should use different
 * prefix names.
 */
public enum YaCyServices implements Services {

    mcp(8100),              // the master connect program which orchestrates all other services
    loader(8200),           // a network resource loader acting (b.o.) as headless browser which is able to enrich http with AJAX content
    crawler(8300),          // a crawler which loads a lot of documents from web or other network resources
    warcmanager(8400),      // a process which combines single WARC files to bigger ones to create archives
    parser(8500),           // a parser service which turns WARC into YaCy JSON
    enricher(8600),         // a semantic enricher for YaCy JSON objects
    indexer(8700),          // a loader which pushes parsed/enriched YaCy JSON content to a search index
    aggregation(8800),      // a search front-end which combines different index sources into one
    moderation(8900),       // a search front-end which for content moderation, i.e. search index account management
    successmessages(10100), // a service which handles the successful operation messages
    errormessages(10200),   // a service which handles failure messages and broken action chains
    ftp(2121),              // a FTP server to be used for mass data / file storage
    samba(445),             // a SMB server to be used for mass data / file storage
    rabbitmq(5672),         // a rabbitmq message queue server to be used for global messages, queues and stacks
    elastic(9300);          // an elasticsearch server or main cluster address for global database storage

    private int default_port;
    
    private YaCyServices(int default_port) {
        this.default_port = default_port;
    }

    @Override
    public int getDefaultPort() {
        return this.default_port;
    }
    
}
