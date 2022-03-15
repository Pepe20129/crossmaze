package io.github.pepe20129.crossmaze.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowEntity.class)
public class ArrowEntityMixin extends PersistentProjectileEntity {
	protected ArrowEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void killLandedArrows(CallbackInfo ci) {
		if (inGround)
			discard();
	}

	@Override
	public ItemStack asItemStack() {
		return null;
	}
}