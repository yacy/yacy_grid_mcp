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

import net.yacy.grid.io.messages.GridQueue;

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

    mcp(8100, new GridQueue[]{       // the master connect program which orchestrates all other services
            new GridQueue("inquiry_open"),
            new GridQueue("inquiry_working"),
            new GridQueue("inquiry_close")
    }, new String[0]),
    indexer(8700, new GridQueue[]{  // an uploader which pushes parsed/enriched YaCy JSON content to a search index
            new GridQueue("elasticsearch_00") // indexing is fast, we do no need more queues here
    }, new String[0]),
    parser(8500, new GridQueue[]{   // a parser service which turns WARC into YaCy JSON
            new GridQueue("yacyparser_00") // parsing is fast, we do not need more queues here
    }, new String[] {"indexer" /*, "crawler"*/}), // omitting the crawler to get the opportunity to flush all queues if they are too full
    loader(8200, new GridQueue[]{
            new GridQueue("webloader_00"), new GridQueue("webloader_01"), new GridQueue("webloader_02"), new GridQueue("webloader_03"),
            new GridQueue("webloader_04"), new GridQueue("webloader_05"), new GridQueue("webloader_06"), new GridQueue("webloader_07"),
            new GridQueue("webloader_08"), new GridQueue("webloader_09"), new GridQueue("webloader_10"), new GridQueue("webloader_11"),
            new GridQueue("webloader_12"), new GridQueue("webloader_13"), new GridQueue("webloader_14"), new GridQueue("webloader_15"),
            new GridQueue("webloader_16"), new GridQueue("webloader_17"), new GridQueue("webloader_18"), new GridQueue("webloader_19"),
            new GridQueue("webloader_20"), new GridQueue("webloader_21"), new GridQueue("webloader_22"), new GridQueue("webloader_23"),
            new GridQueue("webloader_24"), new GridQueue("webloader_25"), new GridQueue("webloader_26"), new GridQueue("webloader_27"),
            new GridQueue("webloader_28"), new GridQueue("webloader_29"), new GridQueue("webloader_30"), new GridQueue("webloader_31")
    }, new String[] {"parser"}),      // a network resource loader acting (b.o.) as headless browser which is able to enrich http with AJAX content
    crawler(8300, new GridQueue[]{  // a crawler which loads a lot of documents from web or other network resources
            new GridQueue("webcrawler_00"), new GridQueue("webcrawler_01"), new GridQueue("webcrawler_02"), new GridQueue("webcrawler_03"),
            new GridQueue("webcrawler_04"), new GridQueue("webcrawler_05"), new GridQueue("webcrawler_06"), new GridQueue("webcrawler_07")
    }, new String[] {"loader"}),
    warcmanager(8400),              // a process which combines single WARC files to bigger ones to create archives
    enricher(8600),                 // a semantic enricher for YaCy JSON objects
    aggregation(8800),              // a search front-end which combines different index sources into one
    moderation(8900),               // a search front-end which for content moderation, i.e. search index account management
    successmessages(10100),         // a service which handles the successful operation messages
    errormessages(10200),           // a service which handles failure messages and broken action chains
    ftp(2121),                      // a FTP server to be used for mass data / file storage
    samba(445),                     // a SMB server to be used for mass data / file storage
    rabbitmq(5672),                 // a rabbitmq message queue server to be used for global messages, queues and stacks
    elastic(9300);                  // an elasticsearch server or main cluster address for global database storage

    private int default_port;
    private GridQueue[] sourceQueues;
    private String[] targetServices; // we cannot use "Services" here as type because that would require a circular declaration

    private YaCyServices(int default_port) {
        this.default_port = default_port;
        this.sourceQueues = null;
    }
    
    private YaCyServices(int default_port, GridQueue[] sourceQueues, String[] targetServices) {
        this.default_port = default_port;
        this.sourceQueues = sourceQueues;
        this.targetServices = targetServices;
    }

    @Override
    public int getDefaultPort() {
        return this.default_port;
    }

    @Override
    public GridQueue[] getSourceQueues() {
        return this.sourceQueues;
    }

    @Override
    public Services[] getTargetServices() {
        Services[] services = new Services[this.targetServices.length];
        for (int i = 0; i < this.targetServices.length; i++) services[i] = YaCyServices.valueOf(this.targetServices[i]);
        return services;
    }

}
