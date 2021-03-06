/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that holds the configuration from the {@link PWA} annotation.
 *
 * Takes {@link PWA} in constructor to fill properties. Sanitizes the input and
 * falls back to default values if {@link PWA} is unavailable ({@code null}).
 */
public class PwaConfiguration implements Serializable {
    public static final String DEFAULT_PATH = "manifest.json";
    public static final String DEFAULT_LOGO = "icons/logo.png";
    public static final String DEFAULT_NAME = "Vaadin Flow Application";
    public static final String DEFAULT_THEME_COLOR = "#ffffff";
    public static final String DEFAULT_BACKGROUND_COLOR = "#f2f2f2";
    public static final String DEFAULT_DISPLAY = "standalone";
    public static final String DEFAULT_OFFLINE_PATH = "offline.html";

    private final String appName;
    private final String shortName;
    private final String description;
    private final String backgroundColor;
    private final String themeColor;
    private final String logoPath;
    private final String manifestPath;
    private final String offlinePath;
    private final String serviceWorkerPath;
    private final String display;
    private final String startUrl;
    private final boolean enabled;
    private final List<String> offlineResources;

    protected PwaConfiguration(PWA pwa, ServletContext servletContext) {
        serviceWorkerPath = "sw.js";
        if (pwa != null) {
            appName = pwa.name();
            shortName = pwa.shortName().substring(0,
                    Math.min(pwa.shortName().length(), 12));
            description = pwa.description();
            backgroundColor = pwa.backgroundColor();
            themeColor = pwa.themeColor();
            logoPath = checkPath(pwa.logoPath());
            manifestPath = checkPath(pwa.manifestPath());
            offlinePath = checkPath(pwa.offlinePath());
            display = pwa.display();
            startUrl = getStartUrl(servletContext);
            enabled = true;
            offlineResources = Arrays.asList(pwa.offlineResources());
        } else {
            appName = DEFAULT_NAME;
            shortName = "Flow PWA";
            description = "";
            backgroundColor = DEFAULT_BACKGROUND_COLOR;
            themeColor = DEFAULT_THEME_COLOR;
            logoPath = DEFAULT_LOGO;
            manifestPath = DEFAULT_PATH;
            offlinePath = DEFAULT_OFFLINE_PATH;
            display = DEFAULT_DISPLAY;
            startUrl = getStartUrl(servletContext);
            enabled = false;
            offlineResources = Collections.emptyList();
        }
    }

    private static String getStartUrl(ServletContext context) {
        return context == null || context.getContextPath() == null
                || context.getContextPath().isEmpty() ? "/"
                        : context.getContextPath() + "/";
    }

    private static String checkPath(String path) {
        return path.replaceAll("^[./]+", "");
    }

    /**
     * Gets the application name.
     *
     * @return application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Gets the application short name.
     *
     * Max 12 characters.
     *
     * @return application short name
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Gets the application description.
     *
     * @return application description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the background color of the application.
     *
     * The background color property is used on the splash screen when the
     * application is first launched.
     *
     * @return background color of the application
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Gets the theme color of the application.
     *
     * The theme color sets the color of the application's tool bar and
     * application's color in the task switcher.
     *
     * @return theme color of the application
     */
    public String getThemeColor() {
        return themeColor;
    }

    /**
     * Gets the path to the application logo file.
     *
     * Example: {@literal img/my-icon.png}
     *
     * @return path to the application logo file
     */
    public String getLogoPath() {
        return logoPath;
    }

    /**
     * Gets the ath to logo with prefix, so request matches.
     *
     * @return path to logo with prefix, so request matches
     */
    public String relLogoPath() {
        return "/" + logoPath;
    }

    /**
     * Gets the path to the manifest.json.
     *
     * @return path to the manifest.json
     */
    public String getManifestPath() {
        return manifestPath;
    }

    /**
     * Path to manifest with prefix, so request matches.
     *
     * @return path to manifest with prefix, so request matches
     */
    public String relManifestPath() {
        return "/" + manifestPath;
    }

    /**
     * Path to static offline html file.
     *
     * @return path to static offline html file
     */
    public String getOfflinePath() {
        return offlinePath;
    }

    /**
     * Path to offline file with prefix, so request matches.
     *
     * @return path to offline file with prefix, so request matches
     */
    public String relOfflinePath() {
        return "/" + offlinePath;
    }

    /**
     * Gets the path to the service worker.
     *
     * @return path to service worker
     */
    public String getServiceWorkerPath() {
        return serviceWorkerPath;
    }

    /**
     * Gets the path to service worker with prefix, so request matches.
     *
     * @return path to service worker with prefix, so request matches
     */
    public String relServiceWorkerPath() {
        return "/" + serviceWorkerPath;
    }

    /**
     * Gets the list of files to be added to pre cache.
     *
     * @return list of files to be added to pre cache
     */
    public List<String> getOfflineResources() {
        return Collections.unmodifiableList(offlineResources);
    }

    /**
     * Gets the the developers’ preferred display mode for the website.
     *
     * Possible values: fullscreen, standalone, minimal-ui, browser
     *
     * @return display mode
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Gets the start url of the PWA application.
     *
     * @return start url of the PWA application
     */
    public String getStartUrl() {
        return startUrl;
    }

    /**
     * Is PWA enabled.
     *
     * @return is PWA enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
