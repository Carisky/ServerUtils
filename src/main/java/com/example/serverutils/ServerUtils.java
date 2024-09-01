package com.example.serverutils;

import com.mojang.datafixers.types.templates.List;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import java.util.*;

@Mod(ServerUtils.MODID)
public class ServerUtils {
    public static final String MODID = "serverutils";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<EntityType<?>> BLACKLIST = new HashSet<>();
    
    // Период очистки в тиках (5 минут = 6000 тиков)
    private static final int CLEAR_INTERVAL_TICKS = 6000;
    private int ticksElapsed = 0;

    private static final int WARNING_INTERVAL_TICKS_5MIN = 6000 * 5; // 5 минут
    private static final int WARNING_INTERVAL_TICKS_1MIN = 6000 * 1; // 1 минута
    private static final int WARNING_INTERVAL_TICKS_5SEC = 100;

    private int ticksUntilNextWarning_5Min = WARNING_INTERVAL_TICKS_5MIN;
    private int ticksUntilNextWarning_1Min = WARNING_INTERVAL_TICKS_1MIN;
    private int ticksUntilNextWarning_5Sec = WARNING_INTERVAL_TICKS_5SEC;

    public ServerUtils() {
        BLACKLIST.add(EntityType.PLAYER);
        BLACKLIST.add(EntityType.VILLAGER);
        BLACKLIST.add(EntityType.ALLAY);
        BLACKLIST.add(EntityType.BOAT);
        BLACKLIST.add(EntityType.MINECART);
        BLACKLIST.add(EntityType.SHULKER);
        BLACKLIST.add(EntityType.SHULKER_BULLET);
        BLACKLIST.add(EntityType.ENDER_DRAGON);
        BLACKLIST.add(EntityType.ENDER_PEARL);
        BLACKLIST.add(EntityType.EYE_OF_ENDER);
        BLACKLIST.add(EntityType.END_CRYSTAL);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ticksElapsed++;
    
            if (ticksElapsed >= CLEAR_INTERVAL_TICKS) {
                ticksElapsed = 0; // Сброс счётчика тиков
    
                MinecraftServer server = event.getServer();
                for (ServerLevel world : server.getAllLevels()) {
                    // Отправляем предупреждения о предстоящей очистке
                    if (ticksUntilNextWarning_5Min <= CLEAR_INTERVAL_TICKS) {
                        sendWarningToPlayers(world, "Entity cleanup will start in 5 minutes!");
                        ticksUntilNextWarning_5Min = WARNING_INTERVAL_TICKS_5MIN; // Сброс таймера
                    } else {
                        ticksUntilNextWarning_5Min -= CLEAR_INTERVAL_TICKS;
                    }
    
                    if (ticksUntilNextWarning_1Min <= CLEAR_INTERVAL_TICKS) {
                        sendWarningToPlayers(world, "Entity cleanup will start in 1 minute!");
                        ticksUntilNextWarning_1Min = WARNING_INTERVAL_TICKS_1MIN; // Сброс таймера
                    } else {
                        ticksUntilNextWarning_1Min -= CLEAR_INTERVAL_TICKS;
                    }
    
                    if (ticksUntilNextWarning_5Sec <= CLEAR_INTERVAL_TICKS) {
                        sendWarningToPlayers(world, "Entity cleanup will start in 5 seconds!");
                        ticksUntilNextWarning_5Sec = WARNING_INTERVAL_TICKS_5SEC; // Сброс таймера
                    } else {
                        ticksUntilNextWarning_5Sec -= CLEAR_INTERVAL_TICKS;
                    }
    
                    clearEntities(world);
                }
            }
        }
    }

    private void sendWarningToPlayers(ServerLevel world, String message) {
        for (ServerPlayer player : world.players()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // Метод для очистки сущностей в указанном мире
    private void clearEntities(ServerLevel world) {
        // Создаем список для хранения сущностей, которые нужно удалить
        java.util.List<Entity> entitiesToRemove = new ArrayList<>();

        // Перебираем все сущности в мире
        for (Entity entity : world.getAllEntities()) {
            // Проверяем, если тип сущности не в чёрном списке
            if (!BLACKLIST.contains(entity.getType())) {
                entitiesToRemove.add(entity);
                LOGGER.info("Marked for removal: {} at {}", entity.getName().getString(), entity.blockPosition());
            }
        }

        // Удаляем сущности после завершения итерации по коллекции
        for (Entity entity : entitiesToRemove) {
            entity.remove(Entity.RemovalReason.DISCARDED); // Удаление сущности
            LOGGER.info("Removed entity: {} at {}", entity.getName().getString(), entity.blockPosition());
        }
    }

}
