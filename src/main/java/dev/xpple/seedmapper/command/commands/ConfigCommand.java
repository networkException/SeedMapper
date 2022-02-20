package dev.xpple.seedmapper.command.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.brigadier.Command;
import com.seedfinding.mccore.block.Block;
import com.seedfinding.mccore.block.Blocks;
import dev.xpple.seedmapper.command.ClientCommand;
import dev.xpple.seedmapper.command.CustomClientCommandSource;
import dev.xpple.seedmapper.util.chat.Chat;
import dev.xpple.seedmapper.util.config.Config;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static dev.xpple.clientarguments.arguments.CColorArgumentType.color;
import static dev.xpple.clientarguments.arguments.CColorArgumentType.getCColor;
import static dev.xpple.seedmapper.SeedMapper.CLIENT;
import static dev.xpple.seedmapper.util.chat.ChatBuilder.*;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;
import static net.minecraft.command.CommandSource.suggestMatching;

public class ConfigCommand extends ClientCommand {

    @Override
    protected void build() {
        argumentBuilder
                .then(literal("automate")
                        .then(literal("set")
                                .then(argument("bool", bool())
                                        .executes(ctx -> setAutomate(CustomClientCommandSource.of(ctx.getSource()), getBool(ctx, "bool")))))
                        .then(literal("get")
                                .executes(ctx -> getAutomate(CustomClientCommandSource.of(ctx.getSource()))))
                        .then(literal("toggle")
                                .executes(ctx -> toggleAutomate(CustomClientCommandSource.of(ctx.getSource())))))
                .then(literal("seed")
                        .then(literal("set")
                                .then(argument("seed", longArg())
                                        .executes(ctx -> setSeed(CustomClientCommandSource.of(ctx.getSource()), getLong(ctx, "seed")))))
                        .then(literal("get")
                                .executes(ctx -> getSeed(CustomClientCommandSource.of(ctx.getSource())))))
                .then(literal("seeds")
                        .then(literal("add")
                                .then(argument("seed", longArg())
                                        .executes(ctx -> addSeed(CustomClientCommandSource.of(ctx.getSource()), getLong(ctx, "seed")))))
                        .then(literal("remove")
                                .then(argument("key", greedyString())
                                        .suggests((context, builder) -> suggestMatching(Config.getSeeds().keySet().stream(), builder))
                                        .executes(ctx -> removeSeed(CustomClientCommandSource.of(ctx.getSource()), getString(ctx, "key"))))))
                .then(literal("ignored")
                        .then(literal("add")
                                .then(argument("block", word())
                                        .suggests((context, builder) -> suggestMatching(Blocks.LATEST_REGISTRY.values().stream().map(Block::getName), builder))
                                        .executes(ctx -> addBlock(CustomClientCommandSource.of(ctx.getSource()), getString(ctx, "block")))))
                        .then(literal("remove")
                                .then(argument("block", word())
                                        .suggests((context, builder) -> suggestMatching(Config.getIgnoredBlocks(), builder))
                                        .executes(ctx -> removeBlock(CustomClientCommandSource.of(ctx.getSource()), getString(ctx, "block")))))
                        .then(literal("list")
                                .executes(ctx -> listBlocks(CustomClientCommandSource.of(ctx.getSource())))))
                .then(literal("colors")
                        .then(literal("add")
                                .then(argument("block", word())
                                        .suggests((context, builder) -> suggestMatching(Blocks.LATEST_REGISTRY.values().stream().map(Block::getName), builder))
                                        .then(argument("color", color())
                                                .executes(ctx -> addColor(CustomClientCommandSource.of(ctx.getSource()), getString(ctx, "block"), getCColor(ctx, "color"))))))
                        .then(literal("remove")
                                .then(argument("block", word())
                                        .suggests((context, builder) -> suggestMatching(Config.getColors().keySet().stream(), builder))
                                        .executes(ctx -> removeColor(CustomClientCommandSource.of(ctx.getSource()), getString(ctx, "block")))))
                        .then(literal("list")
                                .executes(ctx -> listColors(CustomClientCommandSource.of(ctx.getSource())))));
    }

    @Override
    protected String rootLiteral() {
        return "config";
    }

    /**
     * If automation is set to true, new chunks will automatically be compared.
     */
    private int setAutomate(CustomClientCommandSource source, boolean value) {
        Config.toggle("automate", value);
        Chat.print("", new TranslatableText("command.config.setAutomate", value));
        return Command.SINGLE_SUCCESS;
    }

    private int getAutomate(CustomClientCommandSource source) {
        Chat.print("", new TranslatableText("command.config.getAutomate", Config.isEnabled("automate")));
        return Command.SINGLE_SUCCESS;
    }

    private int toggleAutomate(CustomClientCommandSource source) {
        boolean old = Config.isEnabled("automate");
        Config.toggle("automate", !old);
        Chat.print("", new TranslatableText("command.config.toggleAutomate", !old));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * The seed that will be used for the comparison.
     */
    private int setSeed(CustomClientCommandSource source, long seed) {
        Config.set("seed", seed);
        Chat.print("", new TranslatableText("command.config.setSeed", hover(
            format(copy(
                text(String.valueOf(seed)),
                String.valueOf(seed)
            ), Formatting.GREEN),
            new TranslatableText("chat.copy.click")
        )));

        return Command.SINGLE_SUCCESS;
    }

    private int getSeed(CustomClientCommandSource source) {
        JsonElement element = Config.get("seed");
        if (element instanceof JsonNull) {
            Chat.print("", new TranslatableText("command.config.getSeed.null"));
        } else {
            Chat.print("", new TranslatableText("command.config.getSeed", hover(
                format(copy(
                    text(element.getAsString()),
                    element.getAsString()
                ), Formatting.GREEN),
                new TranslatableText("chat.copy.click")
            )));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * If seeds are added to this list, they will be used if the server matches the key.
     * <code>CLIENT.getNetworkHandler().getConnection().getAddress().toString()</code> will be used to compare the key.
     */
    private int addSeed(CustomClientCommandSource source, long seed) {
        String key = CLIENT.getNetworkHandler().getConnection().getAddress().toString();
        if (Config.addSeed(key, seed)) {
            Chat.print("", new TranslatableText("command.config.addSeed.success"));
        } else {
            Chat.print("", new TranslatableText("command.config.addSeed.alreadyAdded"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int removeSeed(CustomClientCommandSource source, String key) {
        if (Config.removeSeed(key)) {
            Chat.print("", new TranslatableText("command.config.removeSeed.success"));
        } else {
            Chat.print("", new TranslatableText("command.config.removeSeed.notAdded"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * If blocks are added to this list, they will be ignored in the overlay process.
     */
    private int addBlock(CustomClientCommandSource source, String block) {
        if (Config.addBlock(block)) {
            Chat.print("", new TranslatableText("command.config.addBlock.success", block));
        } else {
            Chat.print("", new TranslatableText("command.config.addBlock.alreadyIgnored", block));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int removeBlock(CustomClientCommandSource source, String block) {
        if (Config.removeBlock(block)) {
            Chat.print("", new TranslatableText("command.config.removeBlock.success", block));
        } else {
            Chat.print("", new TranslatableText("command.config.removeBlock.notIgnored", block));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int listBlocks(CustomClientCommandSource source) {
        if (Config.getIgnoredBlocks().isEmpty()) {
            Chat.print("", new TranslatableText("command.config.listBlocks.empty"));
        } else {
            Chat.print("", new TranslatableText("command.config.listBlocks", String.join(", ", Config.getIgnoredBlocks())));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * If blocks are added to this map, they will be overlayed with a custom color in the overlay process.
     */
    private int addColor(CustomClientCommandSource source, String block, Formatting color) {
        final int colorValue = color.getColorValue();
        short[] rgbArray = new short[]{(short) ((colorValue >> 16) & 0xFF), (short) ((colorValue >> 8) & 0xFF), (short) (colorValue & 0xFF)};
        if (Config.addColor(block, rgbArray)) {
            Chat.print("", new TranslatableText("command.config.addColor.success", block).append(new LiteralText(color.getName()).formatted(color)).append("."));
        } else {
            Chat.print("", new TranslatableText("command.config.addColor.alreadyAdded", block));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int removeColor(CustomClientCommandSource source, String block) {
        if (Config.removeColor(block)) {
            Chat.print("", new TranslatableText("command.config.removeColor.success", block));
        } else {
            Chat.print("", new TranslatableText("command.config.removeColor.notAdded", block));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int listColors(CustomClientCommandSource source) {
        Map<String, short[]> map = Config.getColors();
        if (map.isEmpty()) {
            Chat.print("", new TranslatableText("command.config.listColors.empty"));
        } else {
            Chat.print("", chain(
                    new TranslatableText("command.config.listColors"),
                    join(highlight(", "), map.entrySet().stream().map(entry -> {
                        final short[] rgbArray = entry.getValue();
                        int rgb;
                        rgb = rgbArray[0];
                        rgb = (rgb << 8) + rgbArray[1];
                        rgb = (rgb << 8) + rgbArray[2];
                        final Integer finalRgb = rgb;
                        return new LiteralText(entry.getKey()).formatted(Arrays.stream(Formatting.values())
                                .filter(f -> Objects.equals(f.getColorValue(), finalRgb))
                                .findFirst().orElse(Formatting.STRIKETHROUGH));
                    }).collect(Collectors.toList())),
                    highlight(".")
            ));
        }
        return Command.SINGLE_SUCCESS;
    }
}
