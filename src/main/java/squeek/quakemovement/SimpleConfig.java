package squeek.quakemovement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// config tool that provides common methods related to configuration, such as saving and loading and config screen generation.
// to use, create a class similar to the one found in Config.java and pass it to this classes load() method on mod initialization, and access the values directly through the class.
// if you want the user to be able to change values in-game, you can generate a screen through makeScreen() and pass the result into modmenu or something.
// right now only doubles and booleans have list widgets and json serialization. more must be added if you need strings or integers or whatever.
public class SimpleConfig {

    private static final HashMap<String, Object> defaults = new HashMap<>();

    public static void load(Class<?> config) {
        try {
            if (config.getAnnotation(Info.class) == null) {
                System.err.print("Please annotate your config class with @CfgUtils.Info");
                System.exit(-1);
            }

            String filename = config.getAnnotation(Info.class).namespace() + ".json";
            File file = new File(FabricLoader.getInstance().getConfigDir().resolve(filename).toString());

            BufferedReader br = new BufferedReader(new FileReader(file));
            JsonObject json = new JsonParser().parse(br).getAsJsonObject();

            setDefaults(config);

            for (Field field : config.getDeclaredFields()) {
                Object value = field.get(config);

                JsonPrimitive loadedValue = json.getAsJsonPrimitive(field.getName());
                if (loadedValue == null)
                    continue;

                if (value instanceof Double)
                    field.set(config, json.getAsJsonPrimitive(field.getName()).getAsDouble());
                else if (value instanceof Boolean)
                    field.set(config, json.getAsJsonPrimitive(field.getName()).getAsBoolean());
            }
            br.close();
        } catch (IOException err) { // no config, probably, generate one
            save(config);
            setDefaults(config);
        } catch (IllegalAccessException err) {
            err.printStackTrace();
        }
    }

    private static void save(Class<?> config) {
        try {
            JsonObject json = new JsonObject();
            for (Field field : config.getDeclaredFields()) {
                Object value = field.get(Config.class);

                if (value instanceof Double)
                    json.addProperty(field.getName(), (Double) value);
                else if (value instanceof Boolean)
                    json.addProperty(field.getName(), (boolean) value);
            }

            String filename = config.getAnnotation(Info.class).namespace() + ".json";
            File file = new File(FabricLoader.getInstance().getConfigDir().resolve(filename).toString());

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(json.toString().replaceAll(":", ": ")
                    .replaceAll(",", ",\n  ")
                    .replaceAll("\\{", "{\n  ")
                    .replaceAll("}", "\n}"));
            fileWriter.close();
        } catch (IOException | IllegalAccessException err) {   // saving failure (really bad xd)
            err.printStackTrace();
        }
    }

    private static Object getDefault(Class<?> config, Field field) {
        return defaults.get(config.getName() + field.getName());
    }

    private static void setDefaults(Class<?> config) {
        try {
            for (Field field : config.getDeclaredFields()) {
                defaults.put(config.getName() + field.getName(), field.get(config));
            }
        } catch (IllegalAccessException err) {
            err.printStackTrace();
        }
    }

    public static Screen makeScreen(Class<?> config, Screen parent) {
        return new ConfigGUI(parent, config);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Category {
        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Info {
        String namespace();
    }

    // screen class, contains widget generating functions and gui definitions
    // not super pretty but minecraft doesn't make it very simple
    private static class ConfigGUI extends Screen {

        private final Screen parent;
        private final Class<?> config;

        protected ConfigGUI(Screen parent, Class<?> config) {
            super(new LiteralText(capitalize(config.getAnnotation(Info.class).namespace()) + " Config"));
            this.parent = parent;
            this.config = config;
        }

        private static String capitalize(String str) {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        @Override
        protected void init() {
            super.init();
            addDrawableChild(new ListWidget(this, client, config));
            addDrawableChild(new ButtonWidget(width / 2 - 154, height - 28, 150, 20, new TranslatableText("gui.back"), button -> client.setScreen(parent)));
            addDrawableChild(new ButtonWidget(width / 2 + 4, height - 28, 150, 20, new TranslatableText("gui.save"), button -> save(config)));
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.render(matrices, mouseX, mouseY, delta);
            drawCenteredText(matrices, textRenderer, title, width / 2, 15, 16777215);
        }

        // list widget which generates and displays all generated entries
        private static class ListWidget extends ElementListWidget<Entry> {

            public ListWidget(Screen parent, MinecraftClient client, Class<?> config) {
                super(client, parent.width + 45, parent.height, 36, parent.height - 36, 20);

                try {
                    for (Field field : config.getDeclaredFields()) {
                        Object value = field.get(config);
                        String generatedKey = "config." + config.getAnnotation(Info.class).namespace() + "." + field.getName().replaceAll("_", ".").toLowerCase();

                        if (field.getAnnotation(SimpleConfig.Category.class) != null)
                            addEntry(new CategoryEntry(new TranslatableText("category." + config.getAnnotation(Info.class).namespace() + "." + field.getAnnotation(Category.class).name())));

                        if (value instanceof Boolean)
                            this.addEntry(new BooleanEntry(new TranslatableText(generatedKey), field, config));
                        else if (value instanceof Double)
                            this.addEntry(new DoubleEntry(new TranslatableText(generatedKey), field, config));
                    }
                } catch (IllegalAccessException err) {
                    err.printStackTrace();
                }
            }

            @Override
            protected int getScrollbarPositionX() {
                return super.getScrollbarPositionX() + 15;
            }

            @Override
            public int getRowWidth() {
                return super.getRowWidth() + 150;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                this.children().forEach(entry -> {
                    if (entry instanceof DoubleEntry) {
                        ((DoubleEntry) entry).textField.setTextFieldFocused(((DoubleEntry) entry).textField.isMouseOver(mouseX, mouseY));
                    }
                });
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        // generic list entry
        private abstract static class Entry extends ElementListWidget.Entry<Entry> {
            protected final List<ClickableWidget> children = new ArrayList<>(0);
            protected final ButtonWidget reset;
            protected final Field field;
            protected final Class<?> config;
            protected final Text display;

            public Entry(Text display, Field field, Class<?> config) {
                this.display = display;
                this.field = field;
                this.config = config;
                reset = new ButtonWidget(0, 0, 20, 20, new LiteralText("R"), button -> reset());
                addChild(reset);
            }

            protected void genericRender(MatrixStack matrices, int y, int x, int entryHeight, int mouseX, int mouseY, float tickDelta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                textRenderer.draw(matrices, display, (float) x, (float) (y + entryHeight / 2 - 5), 16777215);

                reset.x = x + 293;
                reset.y = y;
                reset.active = !isDefault();
                reset.render(matrices, mouseX, mouseY, tickDelta);
            }

            protected void reset() {
            }

            protected boolean isDefault() {
                return false;
            }

            protected void addChild(ClickableWidget child) {
                children.add(child);
            }

            @Override
            public List<? extends Element> children() {
                return children;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return children;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean out = false;
                for (ClickableWidget child : children) {
                    out = out || child.mouseClicked(mouseX, mouseY, button);
                }
                return out;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                boolean out = false;
                for (ClickableWidget child : children) {
                    out = out || child.mouseReleased(mouseX, mouseY, button);
                }
                return out;
            }
        }

        // category entry, generated by annotation
        private static class CategoryEntry extends Entry {

            public CategoryEntry(Text text) {
                super(text, null, null);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                int textWidth = textRenderer.getWidth(display);
                float xPos = (float) (MinecraftClient.getInstance().currentScreen.width / 2 - textWidth / 2);
                textRenderer.draw(matrices, display, xPos, (float) (y + entryHeight - 8), 16777215);
            }
        }

        // boolean entry, generated by boolean field
        private static class BooleanEntry extends Entry {
            private final ButtonWidget toggle;

            public BooleanEntry(Text display, Field field, Class<?> config) {
                super(display, field, config);

                this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, button -> toggle());
                addChild(toggle);
            }

            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                toggle.x = x + 218;
                toggle.y = y;
                toggle.setMessage(new TranslatableText("gui." + String.valueOf(this.get())));
                toggle.render(matrices, mouseX, mouseY, tickDelta);

                genericRender(matrices, y, x, entryHeight, mouseX, mouseY, tickDelta);
            }

            private void toggle() {
                try {
                    field.setBoolean(config, !field.getBoolean(config));
                } catch (Exception ignored) {
                }
            }

            private boolean get() {
                try {
                    return field.getBoolean(config);
                } catch (Exception ignored) {
                    return false;
                }
            }

            @Override
            protected void reset() {
                toggle();
            }

            @Override
            protected boolean isDefault() {
                return get() == (boolean) getDefault(config, field);
            }
        }

        // double entry, generated by double field
        private static class DoubleEntry extends Entry {

            private final TextFieldWidget textField;

            public DoubleEntry(Text display, Field field, Class<?> config) {
                super(display, field, config);

                textField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 70, 16, new LiteralText(get().toString()));
                textField.setText(get().toString());
                textField.setChangedListener((textField) -> set(Double.valueOf(textField.equals("") || textField.equals(".") ? "0" : textField)));
                textField.setTextPredicate(s -> s.matches("^-?[0-9]*\\.?[0-9]*$"));
                addChild(textField);
            }

            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                textField.x = x + 220;
                textField.y = y + 2;
                textField.render(matrices, mouseX, mouseY, tickDelta);

                genericRender(matrices, y, x, entryHeight, mouseX, mouseY, tickDelta);
            }

            private void set(Double number) {
                try {
                    field.setDouble(config, number);
                } catch (Exception ignored) {
                }
            }

            private Double get() {
                try {
                    return field.getDouble(config);
                } catch (Exception ignored) {
                    return 0D;
                }
            }

            @Override
            protected void reset() {
                set((Double) getDefault(config, field));
                textField.setText(getDefault(config, field).toString());
            }

            @Override
            protected boolean isDefault() {
                return get().equals(getDefault(config, field));
            }

            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return super.keyPressed(keyCode, scanCode, modifiers) || textField.keyPressed(keyCode, scanCode, modifiers);
            }

            public boolean charTyped(char chr, int modifiers) {
                return textField.charTyped(chr, modifiers);
            }
        }
    }
}