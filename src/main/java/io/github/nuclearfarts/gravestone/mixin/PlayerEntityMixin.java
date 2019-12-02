package io.github.nuclearfarts.gravestone.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.github.nuclearfarts.gravestone.GravestoneBlockEntity;
import io.github.nuclearfarts.gravestone.GravestoneMod;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	
	private PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
		super(type, world);
	}

	@Redirect(at = @At(value = "INVOKE", target = "drop(Lnet/minecraft/entity/damage/DamageSource;)V"), method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V")
	private void dropRedirect(ServerPlayerEntity self, DamageSource source) {
		
		//PlayerEntity self = (PlayerEntity)(LivingEntity)this;
		List<ItemStack> items = new ArrayList<>();
		items.addAll(self.inventory.main);
		items.addAll(self.inventory.armor);
		items.addAll(self.inventory.offHand);
		self.inventory.main.clear();
		self.inventory.armor.clear();
		self.inventory.offHand.clear();
		
		BlockPos gravePos = findGravePos();
		world.setBlockState(gravePos, GravestoneMod.GRAVESTONE.getDefaultState());
		GravestoneBlockEntity be = (GravestoneBlockEntity) world.getBlockEntity(gravePos);
		be.inventory = items;
		be.markDirty();
		
		this.drop(source); //if a mod adds custom equipment, it might need this to drop properly.
	}
	
	@Unique
	private BlockPos findGravePos() {
		BlockPos unclamped = this.getBlockPos();
		BlockPos playerPos = new BlockPos(unclamped.getX(), clampY(unclamped.getY()), unclamped.getZ());
		if(canPlaceGrave(playerPos)) {
			return playerPos;
		}
		BlockPos.Mutable checkPos = new BlockPos.Mutable();
		for(int x = playerPos.getX() + 5; x >= playerPos.getX() - 5; x--) {
			checkPos.setX(x);
			for(int y = clampY(playerPos.getY()) + 5; x >= MathHelper.clamp(playerPos.getY() - 5, 1, 255); y--) {
				checkPos.setY(y);
				for(int z = playerPos.getZ() + 5; x >= playerPos.getZ() + 5; z--) {
					checkPos.setZ(z);
					if(canPlaceGrave(checkPos)) {
						return checkPos.toImmutable();
					}
				}
			}
		}
		checkPos.set(playerPos);
		while(world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK) {
			checkPos.setY(checkPos.getY() + 1);
		}
		return checkPos.toImmutable();
	}
	
	@Unique
	private int clampY(int y) {
		if(this.dimension == DimensionType.THE_NETHER && y < 127) { //don't spawn on nether ceiling, unless the player is already there.
			return MathHelper.clamp(y, 1, 126); //clamp to 1 -- don't spawn graves the layer right above the void, so players can actually recover their items.
		} else {
			return MathHelper.clamp(y, 1, 255);
		}
	}
	
	@Unique
	private boolean canPlaceGrave(BlockPos pos) {
		return !(pos.getY() < 0 || pos.getY() > 255) && (world.getBlockState(pos).isAir() || world.getBlockState(pos).getCollisionShape(world, pos).isEmpty());
	}
}
