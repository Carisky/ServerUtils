package com.example.serverutils;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

    // Интервалы в миллисекундах
    private static final long CLEAR_INTERVAL_MILLIS = 5 * 60 * 1000; // 5 минут
    private static final long WARNING_INTERVAL_MILLIS_5MIN = 5 * 60 * 1000; // 5 минут
    private static final long WARNING_INTERVAL_MILLIS_1MIN = 60 * 1000; // 1 минута
    private static final long WARNING_INTERVAL_MILLIS_5SEC = 5 * 1000; // 5 секунд

    private long lastClearTime = 0;
    private long lastWarning5MinTime = 0;
    private long lastWarning1MinTime = 0;
    private long lastWarning5SecTime = 0;

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
        BLACKLIST.add(EntityType.WARDEN);


        MinecraftForge.EVENT_BUS.register(this);
        lastClearTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long currentTime = System.currentTimeMillis();

            // Проверяем и отправляем предупреждения
            if (currentTime - lastClearTime >= CLEAR_INTERVAL_MILLIS - WARNING_INTERVAL_MILLIS_5MIN
                    && currentTime - lastWarning5MinTime >= WARNING_INTERVAL_MILLIS_5MIN) {
                sendWarningToAllPlayers(event.getServer(), "Entity cleanup will start in 5 minutes!");
                lastWarning5MinTime = currentTime;
            }

            if (currentTime - lastClearTime >= CLEAR_INTERVAL_MILLIS - WARNING_INTERVAL_MILLIS_1MIN
                    && currentTime - lastWarning1MinTime >= WARNING_INTERVAL_MILLIS_1MIN) {
                sendWarningToAllPlayers(event.getServer(), "Entity cleanup will start in 1 minute!");
                lastWarning1MinTime = currentTime;
            }

            if (currentTime - lastClearTime >= CLEAR_INTERVAL_MILLIS - WARNING_INTERVAL_MILLIS_5SEC
                    && currentTime - lastWarning5SecTime >= WARNING_INTERVAL_MILLIS_5SEC) {
                sendWarningToAllPlayers(event.getServer(), "Entity cleanup will start in 5 seconds!");
                lastWarning5SecTime = currentTime;
            }

            // Проверяем, если прошло достаточно времени для очистки
            if (currentTime - lastClearTime >= CLEAR_INTERVAL_MILLIS) {
                lastClearTime = currentTime; // Обновляем время последней очистки
                clearEntitiesInAllWorlds(event.getServer());
            }
        }
    }

    private void sendWarningToAllPlayers(MinecraftServer server, String message) {
        for (ServerLevel world : server.getAllLevels()) {
            sendMessageToPlayers(world, message);
        }
    }

    private void sendMessageToPlayers(ServerLevel world, String message) {
        for (ServerPlayer player : world.players()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // Метод для очистки сущностей во всех мирах
    private void clearEntitiesInAllWorlds(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            clearEntities(world);
        }
    }

    // Метод для очистки сущностей в указанном мире
    private void clearEntities(ServerLevel world) {
        // Создаем список для хранения сущностей, которые нужно удалить
        List<Entity> entitiesToRemove = new ArrayList<>();

        // Перебираем все сущности в мире
        for (Entity entity : world.getAllEntities()) {
            // Проверяем, если тип сущности не в чёрном списке и максимальное здоровье
            // сущности меньше 60
            if (!BLACKLIST.contains(entity.getType()) && shouldRemoveEntity(entity)) {
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

    // Метод для проверки, нужно ли удалять сущность на основе её максимального
    // здоровья и наличия имени
    private boolean shouldRemoveEntity(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            // Проверяем, если сущность имеет имя (например, бирка с именем)
            if (livingEntity.hasCustomName()) {
                return false; // Не удаляем сущность, если у неё есть имя
            }

            // Получаем максимальное здоровье сущности
            float maxHealth = livingEntity.getMaxHealth();
            return maxHealth < 60.0F; // Возвращаем true, если максимальное здоровье меньше 60
        }

        // Возвращаем true для не-живых сущностей (они не имеют здоровья)
        return true;
    }
}
