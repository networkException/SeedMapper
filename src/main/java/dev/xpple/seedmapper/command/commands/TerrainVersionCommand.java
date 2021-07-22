package dev.xpple.seedmapper.command.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.xpple.seedmapper.command.ClientCommand;
import dev.xpple.seedmapper.command.SharedHelpers;
import dev.xpple.seedmapper.util.chat.Chat;
import dev.xpple.seedmapper.util.maps.SimpleBlockMap;
import kaptainwutax.biomeutils.biome.Biome;
import kaptainwutax.biomeutils.biome.Biomes;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.mcutils.block.Block;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.terrainutils.TerrainGenerator;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static dev.xpple.seedmapper.SeedMapper.CLIENT;
import static dev.xpple.seedmapper.util.chat.ChatBuilder.*;

public class TerrainVersionCommand extends ClientCommand implements SharedHelpers.Exceptions {

    @Override
    protected void register() {
        argumentBuilder
                .executes(this::execute);
    }

    @Override
    protected String rootLiteral() {
        return "terrainversion";
    }

    private int execute(CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {
        long seed = SharedHelpers.getSeed();
        String dimensionPath = CLIENT.world.getRegistryKey().getValue().getPath();
        Dimension dimension = SharedHelpers.getDimension(dimensionPath);

        final AtomicInteger blocks = new AtomicInteger(Integer.MAX_VALUE);
        final AtomicReference<String> version = new AtomicReference<>();

        Arrays.stream(MCVersion.values())
                .filter(mcVersion -> mcVersion.isNewerThan(MCVersion.v1_10_2))
                .forEach(mcVersion -> {
                    BiomeSource biomeSource = BiomeSource.of(dimension, mcVersion, seed);
                    TerrainGenerator generator = TerrainGenerator.of(dimension, biomeSource);
                    SimpleBlockMap map = new SimpleBlockMap(mcVersion, dimension, Biomes.PLAINS);

                    BlockPos.Mutable mutable = new BlockPos.Mutable();
                    final BlockPos playerBlockPos = CLIENT.player.getBlockPos();
                    final WorldChunk chunk = CLIENT.player.world.getChunk(playerBlockPos.getX() >> 4, playerBlockPos.getZ() >> 4);
                    final ChunkPos chunkPos = chunk.getPos();

                    int newBlocks = 0;
                    for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
                        mutable.setX(x);
                        for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                            mutable.setZ(z);
                            final Block[] column = generator.getColumnAt(x, z);
                            final Biome biome = biomeSource.getBiome(x, 0, z);
                            map.setBiome(biome);
                            for (int y = 0; y < column.length; y++) {
                                mutable.setY(y);
                                int seedBlockInt = column[y].getId();
                                int terrainBlockInt = map.get(chunk.getBlockState(mutable).getBlock());
                                if (seedBlockInt == terrainBlockInt) {
                                    continue;
                                }
                                newBlocks++;
                            }
                        }
                    }
                    if (newBlocks < blocks.get()) {
                        blocks.set(newBlocks);
                        version.set(mcVersion.name);
                    }
                });
        if (version.get().startsWith("1")) {
            Chat.print("", chain(
                    accent(version.get()),
                    highlight(new TranslatableText("command.terrainversion.feedback"))
            ));
        } else {
            Chat.print("", highlight(version.get()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
