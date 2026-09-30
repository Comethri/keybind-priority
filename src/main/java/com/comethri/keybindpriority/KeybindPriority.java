package com.comethri.keybindpriority;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = KeybindPriority.MOD_ID, dist = Dist.CLIENT)
public final class KeybindPriority {
    public static final String MOD_ID = "keybind_priority";
    public static final Logger LOG = LogUtils.getLogger();

    public KeybindPriority() {
        PriorityConfig.load();
        if (Boolean.getBoolean(SelfTest.PROPERTY)) SelfTest.register();
    }
}
