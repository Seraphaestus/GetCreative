package amaryllis.get_creative.recipes.precision_assembly;

import amaryllis.get_creative.CustomPackets;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public record ArmAssemblyParticlesPacket(Vec3 pos, ItemStack particleStack) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmAssemblyParticlesPacket> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecs.VEC3, ArmAssemblyParticlesPacket::pos,
            ItemStack.STREAM_CODEC, ArmAssemblyParticlesPacket::particleStack,
            ArmAssemblyParticlesPacket::new
    );

    public ArmAssemblyParticlesPacket(BlockPos pos, ItemStack particleStack) {
        this(VecHelper.getCenterOf(pos).add(0, 0.5, 0), particleStack);
    }

    @Override
    public void handle(LocalPlayer player) {
        ClientLevel level = player.clientLevel;
        var particle = new ItemParticleOption(ParticleTypes.ITEM, particleStack);

        for (int i = 0; i < 20; i++) {
            Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.075f);
            level.addParticle(particle, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CustomPackets.PRECISION_ASSEMBLY_PARTICLES;
    }
}
