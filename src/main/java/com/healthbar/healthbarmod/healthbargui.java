package com.healthbar.healthbarmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = healthbar.MODID, value = Dist.CLIENT)
public class healthbargui {

    private static final Map<Integer, Float> displayedHealth = new HashMap<>();

    private static LivingEntity cachedTarget = null;
    private static UUID lastTargetId = null;
    private static long lastSeenTime = 0L;
    private static final long BUFFER_MS = 500; // 500 ms

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        LivingEntity target = null;

        if (hit instanceof EntityHitResult ehr &&
                ehr.getEntity() instanceof LivingEntity living &&
                !(living instanceof ArmorStand)) {

            target = living;
            cachedTarget = living;
            lastTargetId = living.getUUID();
            lastSeenTime = System.currentTimeMillis();
        }

        if (target == null) {
            if (cachedTarget == null) return;
            if (System.currentTimeMillis() - lastSeenTime > BUFFER_MS) {
                cachedTarget = null;
                lastTargetId = null;
                return;
            }
            target = cachedTarget;
        }

        if (!target.isAlive()) return;

        GuiGraphics gui = event.getGuiGraphics();

        float current = target.getHealth();
        float max = target.getMaxHealth();
        if (max <= 0) return;

        float percent = current / max;

        int width = 180;
        int height = 44;
        int x = 8;
        int y = 8;

        int id = target.getId();
        float last = displayedHealth.getOrDefault(id, current);
        float smooth = lerp(last, current, 0.12f);
        if (Math.abs(smooth - current) < 0.01f) smooth = current;
        displayedHealth.put(id, smooth);

        float smoothPercent = smooth / max;

        int barX = x + 10;
        int barY = y + 18;
        int barWidth = width - 20;
        int barHeight = 10;

        gui.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x22000000);
        gui.fill(x, y, x + width, y + height, 0xCC0B0B0B);
        gui.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xCC101010);

        gui.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF000000);
        gui.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF2A2A2A);

        int filled = Math.round(barWidth * smoothPercent);
        for (int i = 0; i < filled; i++) {
            float t = i / (float) Math.max(1, barWidth - 1);
            gui.fill(barX + i, barY, barX + i + 1, barY + barHeight, gradientColor(t));
        }

        gui.fill(barX, barY, barX + filled, barY + 2, 0x55FFFFFF);

        if (percent <= 0.25f) {
            float pulse = (float) ((Math.sin(System.currentTimeMillis() / 150.0) + 1) * 0.5);
            int alpha = (int) (60 + pulse * 80);
            gui.fill(barX, barY, barX + filled, barY + barHeight, (alpha << 24) | 0x00FF0000);
        }

        String name = target.getName().getString();
        int nameW = mc.font.width(name);
        gui.drawString(mc.font, name, x + width / 2 - nameW / 2 + 1, y + 5, 0x55000000, false);
        gui.drawString(mc.font, name, x + width / 2 - nameW / 2, y + 4, 0xFFFFFF, false);

        String hp = String.format("%.0f / %.0f", current, max);
        String pct = String.format("%.0f%%", percent * 100f);

        gui.drawString(mc.font, hp, barX, barY + barHeight + 4, 0xE6FFFFFF, false);
        gui.drawString(mc.font, pct,
                x + width - mc.font.width(pct) - 8,
                barY + barHeight + 4,
                0xFFE0E0E0,
                false);

        if (target.hurtTime > 0) {
            gui.fill(x, y, x + width, y + height, 0x33FF0000);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int gradientColor(float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r, g;
        if (t <= 0.5f) {
            float v = t / 0.5f;
            r = (int) (255 * v);
            g = 255;
        } else {
            float v = (t - 0.5f) / 0.5f;
            r = 255;
            g = (int) (255 * (1 - v));
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
