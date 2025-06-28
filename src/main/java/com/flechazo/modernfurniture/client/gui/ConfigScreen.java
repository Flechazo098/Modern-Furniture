package com.flechazo.modernfurniture.client.gui;

import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.network.NetworkHandler;
import com.flechazo.modernfurniture.network.module.ConfigPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends Screen {
    // Stores widget references for current page
    private final Map<String, AbstractWidget> configWidgets = new HashMap<>();
    // Stores modified configuration values across pages
    private final Map<String, Object> modifiedConfigCache = new HashMap<>();
    // Original configuration entries
    private final List<Map.Entry<String, Object>> configEntries;

    // Pagination variables
    private int currentPage = 0;
    private int itemsPerPage = 0;
    private int totalPages = 0;
    private int panelHeight = 0;

    // UI layout constants
    private static final int ITEM_HEIGHT = 25;
    private static final int MARGIN = 10;
    private static final int LABEL_WIDTH = 150;
    private static final int CONTROL_WIDTH = 100;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PANEL_TOP = 30;
    private static final int PANEL_BOTTOM_MARGIN = 50;

    public ConfigScreen(Map<String, Object> serverConfig) {
        super(Component.literal("Modern Furniture Config"));
        this.configEntries = new ArrayList<>(serverConfig.entrySet());
    }

    @Override
    protected void init() {
        super.init();

        // Calculate panel height based on screen size
        panelHeight = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;

        // Calculate how many items can fit per page
        itemsPerPage = Math.max(1, panelHeight / ITEM_HEIGHT);
        totalPages = (int) Math.ceil((double) configEntries.size() / itemsPerPage);

        // Create page navigation buttons
        createNavigationButtons();

        // Create config widgets for current page
        createPageWidgets();
    }

    private void createNavigationButtons() {
        // Previous page button
        addRenderableWidget(Button.builder(
                        Component.literal("<< Prev"),
                        button -> {
                            if (currentPage > 0) {
                                // Save current page changes before switching
                                saveCurrentPageChanges();
                                currentPage--;
                                clearWidgets();
                                init();
                            }
                        })
                .bounds(width / 2 - 130, height - 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Page indicator showing current/total pages
        addRenderableWidget(new StringWidget(
                width / 2 - 30,
                height - 30,
                60,
                20,
                Component.literal((currentPage + 1) + "/" + totalPages),
                font
        ));

        // Next page button
        addRenderableWidget(Button.builder(
                        Component.literal("Next >>"),
                        button -> {
                            if (currentPage < totalPages - 1) {
                                // Save current page changes before switching
                                saveCurrentPageChanges();
                                currentPage++;
                                clearWidgets();
                                init();
                            }
                        })
                .bounds(width / 2 + 70, height - 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Action buttons
        addRenderableWidget(Button.builder(
                                Component.literal("Save"),
                                button -> saveConfig()
                        )
                        .bounds(width / 2 - 105, height - 60, 100, BUTTON_HEIGHT)
                        .build()
        );

        addRenderableWidget(Button.builder(
                                Component.literal("Cancel"),
                                button -> onClose()
                        )
                        .bounds(width / 2 + 5, height - 60, 100, BUTTON_HEIGHT)
                        .build()
        );
    }

    private void createPageWidgets() {
        // Calculate start and end indices for current page
        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, configEntries.size());

        // Create config items for current page
        int yPos = PANEL_TOP;
        for (int i = startIdx; i < endIdx; i++) {
            Map.Entry<String, Object> entry = configEntries.get(i);
            String key = entry.getKey();

            // Use cached value if modified, otherwise use original value
            Object value = modifiedConfigCache.containsKey(key) ?
                    modifiedConfigCache.get(key) :
                    entry.getValue();

            // Add label
            String label = formatConfigKey(key);
            AbstractWidget labelWidget = new StringWidget(
                    MARGIN,
                    yPos,
                    LABEL_WIDTH,
                    font.lineHeight,
                    Component.literal(label),
                    font
            );
            addRenderableWidget(labelWidget);

            // Create control widget based on value type
            AbstractWidget controlWidget = createControlWidget(key, value, yPos);
            addRenderableWidget(controlWidget);
            configWidgets.put(key, controlWidget);

            yPos += ITEM_HEIGHT;
        }
    }

    private AbstractWidget createControlWidget(String key, Object value, int yPos) {
        int controlX = MARGIN + LABEL_WIDTH + 10;

        if (value instanceof Boolean) {
            return new Checkbox(
                    controlX,
                    yPos,
                    CONTROL_WIDTH,
                    20,
                    Component.literal(""),
                    (Boolean) value,
                    true
            );
        } else if (value instanceof Integer || value instanceof Long) {
            long min = 0;
            long max = 10000;
            Pair<Number, Number> range = ConfigManager.getRange(key);
            if (range != null) {
                min = range.getFirst().longValue();
                max = range.getSecond().longValue();
            }

            return new ForgeSlider(
                    controlX, yPos, CONTROL_WIDTH, 20,
                    Component.literal(""), Component.literal(""),
                    min, max, ((Number) value).longValue(), true
            );
        } else if (value instanceof Float || value instanceof Double) {
            double min = 0;
            double max = 10000;
            Pair<Number, Number> range = ConfigManager.getRange(key);
            if (range != null) {
                min = range.getFirst().doubleValue();
                max = range.getSecond().doubleValue();
            }
            return new ForgeSlider(
                    controlX, yPos, CONTROL_WIDTH, 20,
                    Component.literal(""), Component.literal(""),
                    min, max, ((Number) value).doubleValue(),
                    0.01, 2, true
            );
        } else if (value instanceof String) {
            EditBox editBox = new EditBox(
                    font,
                    controlX,
                    yPos,
                    CONTROL_WIDTH,
                    20,
                    Component.literal("")
            );
            editBox.setValue((String) value);
            return editBox;
        } else {
            return Button.builder(
                            Component.literal("Unsupported"),
                            button -> {
                            }
                    )
                    .bounds(controlX, yPos, CONTROL_WIDTH, 20)
                    .build();
        }
    }

    /**
     * Saves current page widget values to cache only if they differ from original
     * Called before page navigation
     */
    private void saveCurrentPageChanges() {
        for (Map.Entry<String, AbstractWidget> entry : configWidgets.entrySet()) {
            String key = entry.getKey();
            AbstractWidget widget = entry.getValue();
            Object originalValue = getOriginalValue(key);

            if (widget instanceof Checkbox checkbox) {
                boolean currentValue = checkbox.selected();
                if (originalValue instanceof Boolean && (Boolean) originalValue != currentValue) {
                    modifiedConfigCache.put(key, currentValue);
                }
            } else if (widget instanceof ForgeSlider slider) {
                double currentValue = slider.getValue();
                if (originalValue instanceof Number) {
                    double original = ((Number) originalValue).doubleValue();
                    if (Math.abs(original - currentValue) > 0.0001) {
                        modifiedConfigCache.put(key, currentValue);
                    }
                }
            } else if (widget instanceof EditBox editBox) {
                String currentValue = editBox.getValue();
                if (originalValue instanceof String && !originalValue.equals(currentValue)) {
                    modifiedConfigCache.put(key, currentValue);
                }
            }
        }
    }

    /**
     * Gets the original value for a configuration key
     */
    private Object getOriginalValue(String key) {
        for (Map.Entry<String, Object> entry : configEntries) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String formatConfigKey(String key) {
        return key.replaceAll("([A-Z])", " $1")
                .replaceAll("^\\s+", "")
                .replaceAll("\\s+", " ");
    }

    private void saveConfig() {
        // Save current page before final save
        saveCurrentPageChanges();

        // Send all cached modifications to server
        ConfigPacket packet = ConfigPacket.createForUpdate(modifiedConfigCache);
        NetworkHandler.sendToServer(packet);
        onClose();
    }
}
