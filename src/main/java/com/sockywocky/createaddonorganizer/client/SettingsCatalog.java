package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SettingsCatalog {

    public enum Kind { TOGGLE, CHOICE, SLIDER, NUMBER, TEXT, COLOR, LIST }

    public record Option(String key, ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec,
            Kind kind, Component title, Component description, String search) {

        public Object current() {
            return value.get();
        }

        public boolean isDefault() {
            return Objects.equals(value.get(), spec.getDefault());
        }
    }

    public record Group(Component title, List<Option> options) {}

    public record Category(String key, Component label, List<Group> groups) {}

    private static final String PREFIX = createaddonorganizer.MODID + ".configuration.";
    public static final String DEV_SECTION = "devmode";
    private static final long SLIDER_SPAN_LIMIT = 4096L;

    private SettingsCatalog() {}

    public static List<Category> build() {
        ModConfigSpec spec = Config.spec();
        UnmodifiableConfig specTree = spec.getSpec();
        UnmodifiableConfig valueTree = spec.getValues();
        List<Category> categories = new ArrayList<>();
        List<Option> loose = new ArrayList<>();

        for (UnmodifiableConfig.Entry entry : specTree.entrySet()) {
            String key = entry.getKey();
            Object node = entry.getRawValue();
            Object values = valueTree.get(List.of(key));
            if (node instanceof UnmodifiableConfig section && values instanceof UnmodifiableConfig sectionValues) {
                if (DEV_SECTION.equals(key) && !DevMode.isUnlocked()) {
                    continue;
                }
                Category category = category(key, section, sectionValues);
                if (category != null) {
                    categories.add(category);
                }
            } else if (node instanceof ModConfigSpec.ValueSpec valueSpec
                    && values instanceof ModConfigSpec.ConfigValue<?> configValue) {
                loose.add(option(key, configValue, valueSpec));
            }
        }

        if (!loose.isEmpty()) {
            Component label = Component.translatable("createaddonorganizer.settings.general");
            categories.add(0, new Category("general", label, List.of(new Group(label, loose))));
        }
        return categories;
    }

    private static Category category(String key, UnmodifiableConfig section, UnmodifiableConfig values) {
        Component label = label(key);
        List<Group> groups = new ArrayList<>();
        List<Option> direct = leaves(section, values);
        if (!direct.isEmpty()) {
            groups.add(new Group(label, direct));
        }
        for (UnmodifiableConfig.Entry entry : section.entrySet()) {
            if (entry.getRawValue() instanceof UnmodifiableConfig childSpec
                    && values.get(List.of(entry.getKey())) instanceof UnmodifiableConfig childValues) {
                List<Option> options = leaves(childSpec, childValues);
                if (!options.isEmpty()) {
                    groups.add(new Group(label(entry.getKey()), options));
                }
            }
        }
        return groups.isEmpty() ? null : new Category(key, label, groups);
    }

    private static List<Option> leaves(UnmodifiableConfig specTree, UnmodifiableConfig valueTree) {
        List<Option> options = new ArrayList<>();
        for (UnmodifiableConfig.Entry entry : specTree.entrySet()) {
            if (entry.getRawValue() instanceof ModConfigSpec.ValueSpec valueSpec
                    && valueTree.get(List.of(entry.getKey())) instanceof ModConfigSpec.ConfigValue<?> configValue) {
                options.add(option(entry.getKey(), configValue, valueSpec));
            }
        }
        return options;
    }

    private static Option option(String key, ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {
        Component title = label(key);
        Component description = description(key, spec);
        String search = (title.getString() + ' ' + description.getString() + ' ' + key).toLowerCase(Locale.ROOT);
        return new Option(key, value, spec, kindOf(key, spec), title, description, search);
    }

    public static String choiceLabel(Enum<?> value) {
        StringBuilder out = new StringBuilder(value.name().length());
        for (String part : value.name().toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        return out.toString();
    }

    private static Component label(String key) {
        return Component.translatableWithFallback(PREFIX + key, prettify(key));
    }

    private static Component description(String key, ModConfigSpec.ValueSpec spec) {
        String translation = spec.getTranslationKey();
        String lang = (translation != null ? translation : PREFIX + key) + ".tooltip";
        return Component.translatableWithFallback(lang, comment(spec.getComment()));
    }

    private static String comment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("Allowed Values:") || trimmed.startsWith("Range:")) {
                continue;
            }
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(trimmed);
        }
        return text.toString();
    }

    private static Kind kindOf(String key, ModConfigSpec.ValueSpec spec) {
        if (spec instanceof ModConfigSpec.ListValueSpec) {
            return Kind.LIST;
        }
        Class<?> clazz = spec.getClazz();
        Object fallback = spec.getDefault();
        if (clazz == Boolean.class || fallback instanceof Boolean) {
            return Kind.TOGGLE;
        }
        if ((clazz != null && clazz.isEnum()) || fallback instanceof Enum<?>) {
            return Kind.CHOICE;
        }
        if (fallback instanceof Integer) {
            if (isColorKey(key)) {
                return Kind.COLOR;
            }
            ModConfigSpec.Range<Integer> range = spec.getRange();
            if (range != null && (long) range.getMax() - (long) range.getMin() <= SLIDER_SPAN_LIMIT) {
                return Kind.SLIDER;
            }
            return Kind.NUMBER;
        }
        if (fallback instanceof Double || fallback instanceof Float) {
            return spec.getRange() != null ? Kind.SLIDER : Kind.NUMBER;
        }
        if (fallback instanceof Number) {
            return Kind.NUMBER;
        }
        return Kind.TEXT;
    }

    private static boolean isColorKey(String key) {
        return key.toLowerCase(Locale.ROOT).endsWith("color");
    }

    public static Enum<?>[] choices(Option option) {
        Class<?> clazz = option.spec().getClazz();
        if (clazz != null && clazz.isEnum()) {
            return (Enum<?>[]) clazz.getEnumConstants();
        }
        if (option.current() instanceof Enum<?> value) {
            return value.getDeclaringClass().getEnumConstants();
        }
        return new Enum<?>[0];
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void stage(Option option, Object value) {
        ((ModConfigSpec.ConfigValue) option.value()).set(value);
    }

    public static void apply(Option option, Object value) {
        stage(option, value);
        Config.save();
    }

    public static boolean accepts(Option option, Object value) {
        return option.spec().test(value);
    }

    public static boolean acceptsElement(Option option, Object element) {
        if (option.spec() instanceof ModConfigSpec.ListValueSpec list) {
            return list.testElement(element);
        }
        return true;
    }

    public static String newElement(Option option) {
        if (option.spec() instanceof ModConfigSpec.ListValueSpec list && list.getNewElementSupplier() != null) {
            Object created = list.getNewElementSupplier().get();
            return created == null ? "" : String.valueOf(created);
        }
        return "";
    }

    public static List<String> listValue(Option option) {
        List<String> out = new ArrayList<>();
        if (option.current() instanceof List<?> values) {
            for (Object value : values) {
                out.add(String.valueOf(value));
            }
        }
        return out;
    }

    public static String prettify(String key) {
        StringBuilder out = new StringBuilder(key.length() + 6);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (i == 0) {
                out.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c) && !Character.isUpperCase(key.charAt(i - 1))) {
                out.append(' ').append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
