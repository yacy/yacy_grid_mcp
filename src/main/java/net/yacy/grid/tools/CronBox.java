/**
 *  CronBox
 *  Copyright 01.03.2022 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.tools;

import java.util.Random;

/**
 * CronBox is a tool to run periodic processes.
 * This includes the restart of the application itself.
 */
public class CronBox {

    private final static Random random = new Random(System.currentTimeMillis());

    /**
     * Telemetry class which provides information about the running cycle
     */
    public interface Telemetry {

        /**
         * Get information about the ressource allocation.
         * The allocation number is given in percent. The application must compute how much of
         * machine ressources are necessary to run the application and how much is alreay taken
         * from that ressources.
         * @return percent of ressource allocation
         */
        public int getRessourceAllocationPercent();

        /**
         * Get number of deadlocks.
         * This number should always be zero. The actual number can be computed with reflection classes in java
         * @return number of deadlocks in the application.
         */
        public int getDeadlocks();

        /**
         * Count number of OOM appearances and return a frequency as events per day
         * @return count of OOMs per day
         */
        public int getOOMFrequency();

    }

    /**
     * Application interface that users of the CronBox must implement to submit an application for cycling
     */
    public static interface Application extends Runnable {

        /**
         * ask the application to stop
         */
        public void stop();

        /**
         * calls the telemetry computation inside the application
         * @return the current telemetry
         */
        public Telemetry getTelemetry();

    }

    private final static String times(long t) {
        if (t < 1000) return t + " milliseconds";
        t = t / 1000;
        if (t < 60) return t + " seconds";
        t = t / 60;
        if (t < 60) return t + " minutes";
        t = t / 60;
        if (t < 24) return t + " hours";
        t = t / 24;
        if (t < 365) return t + " days";
        t = t / 365;
        return t + " years";
    }

    private final long cycleLen; // sleep time in milliseconds for one cycle
    private final int randomAddon; // maximum sleep time in milliseconds that is randomly added to cycleLen each cycle
    private final Class<? extends Application> applicationClass;

    /**
     * initiates a CronBox with an application class and cycle lengths
     * @param applicationClass a class which will be initiated every time that a cycle starts
     * @param cycleLen the length of the application runtime until stop() is called in milliseconds
     * @param randomAddon a maximum time that is added to each cycleLen in milliseconds
     */
    public CronBox(final Class<? extends Application> applicationClass, final long cycleLen, final int randomAddon) {
        Logger.info("Started CronBox application with cycleLen = " + times(cycleLen) + ", randomAddon = " + times(randomAddon));
        this.applicationClass = applicationClass;
        this.cycleLen = cycleLen;
        this.randomAddon = randomAddon;
    }

    public void cycle() {
        Logger.info("Started CronBox cycling..");
        int cycleCount = 0;

        // we first run a fresh instance of the application before starting a cycle
        try {
            Application application = CronBox.this.applicationClass.newInstance();
            Thread applicationInstance = new Thread(application);
            applicationInstance.setName("CronBox Genesis Thread of " + this.applicationClass.getName());
            applicationInstance.start();

            // this loop is suppose to run forever until a kill event happens
            int randomInc = random.nextInt(CronBox.this.randomAddon);
            long sleep = CronBox.this.cycleLen == Long.MAX_VALUE ? CronBox.this.cycleLen : CronBox.this.cycleLen + randomInc;
            Logger.info("Running cycle " + cycleCount + ", sleep = " + times(sleep) + (CronBox.this.cycleLen == Long.MAX_VALUE ? "" : (", including random: " + times(randomInc))));

            while (true) {
                try {Thread.sleep(sleep);} catch (final InterruptedException e) {}

                // after that long sleep time, the application must be stopped and restarted
                Logger.info("Stopping cycle " + cycleCount);
                application.stop();
                while (applicationInstance.isAlive()) {
                    try {Thread.sleep(1000);} catch (final InterruptedException e) {}
                }

                // restart
                cycleCount++;
                application = CronBox.this.applicationClass.newInstance();
                applicationInstance = new Thread(application);
                applicationInstance.setName("CronBox Thread Iteration " + cycleCount + " of " + this.applicationClass.getName());
                applicationInstance.start();

                // calculate new sleep target
                randomInc = random.nextInt(CronBox.this.randomAddon);
                sleep = CronBox.this.cycleLen == Long.MAX_VALUE ? CronBox.this.cycleLen : CronBox.this.cycleLen + randomInc;
                Logger.info("Running cycle " + cycleCount + ", sleep = " + times(sleep) + (CronBox.this.cycleLen == Long.MAX_VALUE ? "" : (", including random: " + times(randomInc))));
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
