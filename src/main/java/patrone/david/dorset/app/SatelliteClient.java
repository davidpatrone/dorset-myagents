/*
 * Copyright 2016 The Johns Hopkins University Applied Physics Laboratory LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package patrone.david.dorset.app;

import edu.jhuapl.dorset.Application;
import edu.jhuapl.dorset.routing.SingleAgentRouter;
import patrone.david.dorset.agent.SatellitePassAgent;
import patrone.david.dorset.client.SimpleClientUI;

/**
 *
 */
public class SatelliteClient {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Application app = new Application(
                        new SingleAgentRouter(new SatellitePassAgent(39.0, -77.0, 200)));
        SimpleClientUI ui = new SimpleClientUI("Satellite Client", app);
        ui.setVisible(true);
    }

}
