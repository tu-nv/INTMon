/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.intmon.ui;

import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import org.onosproject.ui.topo.TopoConstants.Glyphs;

import static org.onosproject.ui.topo.TopoConstants.Properties.FLOWS;
import static org.onosproject.ui.topo.TopoConstants.Properties.INTENTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.LATITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.LONGITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.TOPOLOGY_SSCS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TUNNELS;
import static org.onosproject.ui.topo.TopoConstants.Properties.VERSION;

/**
 * Our topology overlay.
 */
public class IntMonUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in intMonTopov.js
    private static final String OVERLAY_ID = "int-mon-topov-overlay";

    private static final String MY_TITLE = "IntMon Topo View";
    private static final String MY_VERSION = "1.0.0";
    private static final String MY_DEVICE_TITLE = "Device";
    private static final String URI_FRAGMENT = "Short ID";

    private static final ButtonId FOO_BUTTON = new ButtonId("foo");
    private static final ButtonId BAR_BUTTON = new ButtonId("bar");

    public IntMonUiTopovOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.title(MY_TITLE)
                .typeId(Glyphs.CROWN)
                .removeProps(
                        TOPOLOGY_SSCS,
                        INTENTS,
                        TUNNELS,
                        FLOWS,
                        VERSION
                )
                .addProp(VERSION, MY_VERSION);
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.title(MY_DEVICE_TITLE);
//        pp.addProp(URI_FRAGMENT, URI_FRAGMENT);
        pp.removeProps(LATITUDE, LONGITUDE);
        pp.addProp(URI_FRAGMENT, deviceId.uri().getFragment());

        pp.addButton(FOO_BUTTON)
                .addButton(BAR_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);
    }

}
