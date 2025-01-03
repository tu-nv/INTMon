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

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Skeletal ONOS UI Topology-Overlay application component.
 */
@Component(immediate = true)
public class IntMonUiTopovComponent {

    private static final ClassLoader CL = IntMonUiTopovComponent.class.getClassLoader();
    private static final String VIEW_ID = "intMonTopov";
    private static final String VIEW_TEXT = "Sample Topov";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiViewHidden(VIEW_ID)
    );
//    private final List<UiView> uiViews = ImmutableList.of(
//            new UiView(UiView.Category.OTHER, VIEW_ID, VIEW_TEXT)
//    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new IntMonUiTopovMessageHandler()
            );

    // Factory for UI topology overlays
    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(
                    new IntMonUiTopovOverlay()
            );

    // Application UI extension
    protected UiExtension extension =
            new UiExtension.Builder(CL, uiViews)
                    .resourcePath(VIEW_ID)
                    .messageHandlerFactory(messageHandlerFactory)
                    .topoOverlayFactory(topoOverlayFactory)
                    .build();

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }

}
