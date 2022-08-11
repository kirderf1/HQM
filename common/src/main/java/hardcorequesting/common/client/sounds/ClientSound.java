package hardcorequesting.common.client.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class ClientSound extends SimpleSoundInstance {
    
    public ClientSound(ResourceLocation resource, float volume, float pitch) {
        super(resource, SoundSource.BLOCKS, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, Attenuation.NONE, 0, 0, 0, false);
    }
}
