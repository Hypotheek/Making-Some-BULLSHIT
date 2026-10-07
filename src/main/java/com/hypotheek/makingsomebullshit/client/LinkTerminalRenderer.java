package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import com.hypotheek.makingsomebullshit.registry.LinkTerminalBlock;
import com.hypotheek.makingsomebullshit.registry.LinkTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class LinkTerminalRenderer implements BlockEntityRenderer<LinkTerminalBlockEntity> {

    private final BlockRenderDispatcher dispatcher;

    public static final ResourceLocation ORB_ON = new ResourceLocation(MakingSomeBullshit.MOD_ID, "block/link_terminal_orb");

    public static final ResourceLocation ORB_OFF = new ResourceLocation(MakingSomeBullshit.MOD_ID, "block/link_terminal_orb_off");


    public LinkTerminalRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LinkTerminalBlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Level level = blockEntity.getLevel();
        if(level == null) return;

        BlockState state = blockEntity.getBlockState();
        boolean lit = state.getValue(LinkTerminalBlock.LIT);
        Direction facing = state.getValue(LinkTerminalBlock.FACING);

        float time = level.getGameTime() + partialTick;
        float bob = Mth.sin(time * 0.1f) * (1.5f / 16f);

        pose.pushPose();

        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180f)));
        pose.translate(-0.5, bob, -0.5);

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(lit ? ORB_ON : ORB_OFF);

        dispatcher.getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()), state, model, 1f, 1f, 1f, lit ? LightTexture.FULL_BRIGHT : light, overlay);
        pose.popPose();
    }
}
