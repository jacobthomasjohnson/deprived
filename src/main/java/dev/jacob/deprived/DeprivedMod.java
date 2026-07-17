package dev.jacob.deprived;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeprivedMod implements ModInitializer {
    public static final String MOD_ID = "deprived";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int DAY_TICKS = 24_000;
    private static final Map<UUID, DreadState> STATES = new HashMap<>();
    private static DeprivedConfig config;
    private static int tickCounter;

    public static final Block WEEPING_STONE = new Block(BlockBehaviour.Properties.of().strength(2.5F, 8.0F).sound(SoundType.DEEPSLATE).lightLevel(state -> 1));
    public static final Item CURSED_RELIC = new CursedRelicItem(new Item.Properties().stacksTo(1).durability(13).rarity(Rarity.EPIC));
    public static final SoundEvent HAUNTED_FOOTSTEP = sound("haunted_footstep");
    public static final SoundEvent KNOCK = sound("knock");
    public static final SoundEvent SCRATCH = sound("scratch");
    public static final SoundEvent BREATH = sound("breath");
    public static final SoundEvent WHISPER = sound("whisper");
    public static final SoundEvent DOOR = sound("door");
    public static final SoundEvent IMPACT = sound("impact");
    public static final SoundEvent DRONE = sound("drone");
    public static final SoundEvent RADIO_SIGNAL = sound("radio_signal");
    public static final SoundEvent DISTANT_SINGING = sound("distant_singing");
    public static final SoundEvent RISING_DREAD = sound("rising_dread");
    public static final SoundEvent DEEP_DRONE = sound("deep_drone");

    @Override
    public void onInitialize() {
        config = DeprivedConfig.load();
        ConfigPacket.register();
        Registry.register(BuiltInRegistries.BLOCK, id("weeping_stone"), WEEPING_STONE);
        Registry.register(BuiltInRegistries.ITEM, id("weeping_stone"), new BlockItem(WEEPING_STONE, new Item.Properties()));
        Registry.register(BuiltInRegistries.ITEM, id("cursed_relic"), CURSED_RELIC);
        registerSound("haunted_footstep", HAUNTED_FOOTSTEP); registerSound("knock", KNOCK); registerSound("scratch", SCRATCH);
        registerSound("breath", BREATH); registerSound("whisper", WHISPER); registerSound("door", DOOR);
        registerSound("impact", IMPACT); registerSound("drone", DRONE); registerSound("radio_signal",RADIO_SIGNAL);
        registerSound("distant_singing",DISTANT_SINGING); registerSound("rising_dread",RISING_DREAD); registerSound("deep_drone",DEEP_DRONE);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> entries.accept(WEEPING_STONE));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(CURSED_RELIC));
        ServerTickEvents.END_SERVER_TICK.register(DeprivedMod::tick);
        registerCommands();
        LOGGER.info("Deprived 1.3.1: sleep while you still trust the dark.");
    }

    private static void tick(MinecraftServer server) {
        if (!config.enabled) return;
        tickCounter = (tickCounter + 1) % 100;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DreadState state = state(player);
            tickSequences(player, state);
            if (state.cooldownTicks > 0) state.cooldownTicks--;
            if (tickCounter % 20 != 0 || player.isSpectator()) continue;

            int awakeTicks = awakeTicks(player);
            int stage = stageFor(awakeTicks);
            if (awakeTicks < state.lastRestTicks - 200 || stage < state.stage) {
                clearAfterSleep(player, state);
            }
            state.lastRestTicks = awakeTicks;
            state.pressureSeconds++;
            if(!"none".equals(state.lastEvent))state.secondsSinceLastEvent++;
            updateContext(player,state);
            if (stage != state.stage) {
                state.stage = stage;
                state.pressureSeconds = 0;
                state.cooldownTicks = stage == 0 ? 0 : (30 + player.getRandom().nextInt(61)) * 20;
                VisualPacket.sync(player, config.intenseVisuals ? stage : 0);
                LOGGER.info("{} entered deprivation stage {} after {} awake ticks", player.getGameProfile().getName(), stage, awakeTicks);
                if (stage >= 3) playFar(player, DRONE, 20, 36, .24F, .98F);
            } else if (tickCounter == 0) {
                VisualPacket.sync(player, config.intenseVisuals ? stage : 0);
            }
            applyStagePressure(player, state, stage);
            if (stage > 0 && state.cooldownTicks == 0) {
                triggerRandomEvent(player, stage);
                state.cooldownTicks = nextCooldown(player,stage) * 20;
            }
            if(state.debugHud)DebugPacket.send(player,state,awakeTicks);
        }
    }

    private static void clearAfterSleep(ServerPlayer player, DreadState state) {
        HallucinationPacket.clear(player);
        state.clearHallucinations();
        state.stage = 0;
        state.cooldownTicks = 0;
        VisualPacket.sync(player, 0);
    }

    private static void applyStagePressure(ServerPlayer player, DreadState state, int stage) {
        if (stage < 3) return;
        if (stage >= 3 && tickCounter == 0 && player.getRandom().nextInt(18) == 0) visual(player, VisualPacket.BLINK, 8 + stage, .25F + stage * .05F, 0xFF000000);
        if (stage >= 4 && state.pressureSeconds % 60 == 0) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 140, 0, false, false));
        if (stage >= 5 && state.pressureSeconds % 50 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, stage - 5, false, false));
            visual(player, VisualPacket.PULSE, 80, .28F, 0x44300020);
        }
        if (stage >= 6 && state.pressureSeconds % 35 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 180, 0, false, false));
            if (config.extremeStageCausesDamage && player.getRandom().nextInt(7) == 0) player.hurt(player.damageSources().magic(), 1.0F);
        }
    }

    public static void triggerRandomEvent(Player rawPlayer) {
        if (rawPlayer instanceof ServerPlayer player) triggerRandomEvent(player, Math.max(1, state(player).stage));
    }

    private static void triggerRandomEvent(ServerPlayer player, int stage) {
        List<String> pool = new ArrayList<>(switch (stage) {
            case 1 -> List.of("whisper", "knocking", "placement", "drone");
            case 2 -> List.of("whisper", "knocking", "scratching", "placement", "cavein", "drone", "breath", "radio");
            case 3 -> List.of("footsteps", "cold", "scratching", "breath", "watcher", "blink", "cavein", "singing");
            case 4 -> List.of("footsteps", "darkness", "omen", "watcher", "blink", "phantomhit", "scratching", "drone", "singing", "deepdrone");
            case 5 -> List.of("darkness", "omen", "watchers", "falsecreeper", "phantomhit", "blink", "footsteps", "drone", "breath", "radio", "singing", "riser");
            default -> List.of("darkness", "watchers", "falsecreeper", "phantomhit", "collapse", "footsteps", "omen", "scratching", "breath", "drone", "radio", "singing", "riser", "deepdrone");
        });
        SymptomProfile profile=state(player).profile;
        addWeighted(pool,List.of("whisper","knocking","scratching","drone"),profile.auditory()/2);
        addWeighted(pool,List.of("watcher","blink","omen"),profile.visual()/2);
        addWeighted(pool,List.of("placement","footsteps","breath","phantomhit"),profile.paranoia()/2);
        if(stage>=3)addWeighted(pool,List.of("cold","darkness"),profile.physical()/3);

        DreadState state=state(player);
        if(state.contextLight<=5)addWeighted(pool,List.of("watcher","breath","whisper"),2);
        if(state.contextUnderground)addWeighted(pool,List.of("scratching","cavein","footsteps"),3);
        if(state.contextUnderground)addWeighted(pool,List.of("radio","deepdrone"),1);
        if(state.contextIsolated)addWeighted(pool,List.of("whisper","watcher","placement","singing"),2);
        if(state.contextStorm)addWeighted(pool,List.of("knocking","placement","drone","radio"),2);
        state.lastPoolSize=pool.size();
        triggerEvent(player, pool.get(player.getRandom().nextInt(pool.size())));
    }

    private static void addWeighted(List<String> pool,List<String> events,int weight){for(int i=0;i<weight;i++)pool.addAll(events);}
    private static void updateContext(ServerPlayer player,DreadState state){
        BlockPos position=player.blockPosition();
        state.contextLight=player.level().getMaxLocalRawBrightness(position);
        state.contextUnderground=!player.level().canSeeSky(position)&&position.getY()<55;
        state.contextIsolated=player.server.getPlayerList().getPlayers().stream().noneMatch(other->other!=player&&other.distanceToSqr(player)<4096);
        state.contextStorm=player.serverLevel().isRainingAt(position);
    }

    private static boolean triggerEvent(Player rawPlayer, String event) {
        if (!(rawPlayer instanceof ServerPlayer player)) return false;
        if (event.equals("random")) { triggerRandomEvent(player,Math.max(1,state(player).stage)); return true; }
        ServerLevel level = player.serverLevel();
        int stage = Math.max(1, state(player).stage);
        DreadState debugState=state(player);debugState.lastEvent=event;debugState.secondsSinceLastEvent=0;
        LOGGER.info("Triggering deprivation event '{}' for {} at stage {}", event, player.getGameProfile().getName(), stage);
        switch (event) {
            case "whisper" -> playFar(player, WHISPER, stage < 3 ? 22 : 12, stage < 3 ? 48 : 28, familyVolume(.38F,config.voiceVolumePercent), .98F);
            case "knocking" -> startDisturbance(player, 1, 8);
            case "scratching" -> startDisturbance(player, 2, 9);
            case "placement" -> playFar(player, DOOR, stage < 3 ? 22 : 14, stage < 3 ? 42 : 28, .46F, .99F);
            case "drone" -> playFar(player, DRONE, 28, 56, familyVolume(.24F,config.ambienceVolumePercent), .99F);
            case "radio" -> playFar(player,RADIO_SIGNAL,42,72,familyVolume(.18F,config.ambienceVolumePercent),.99F);
            case "singing" -> playFar(player,DISTANT_SINGING,48,78,familyVolume(.20F,config.voiceVolumePercent),.98F);
            case "riser" -> { playFar(player,RISING_DREAD,46,76,familyVolume(.18F,config.ambienceVolumePercent),.98F); visual(player,VisualPacket.PULSE,180,.28F,0x44383048); }
            case "deepdrone" -> playFar(player,DEEP_DRONE,54,86,familyVolume(.14F,config.ambienceVolumePercent),.99F);
            case "cavein" -> {
                BlockPos pos = offset(player, stage < 3 ? 12 : 4, stage < 3 ? 28 : 10).above(3);
                level.playSound(null, pos, IMPACT, SoundSource.HOSTILE, 1.25F, .94F);
                level.sendParticles(ParticleTypes.ASH, pos.getX()+.5, pos.getY(), pos.getZ()+.5, 120, 2, 2, 2, .05);
                if (stage >= 3) visual(player, VisualPacket.STATIC, 45, .32F, 0x55302030);
            }
            case "breath" -> {
                BlockPos pos = behind(player, 1.0);
                level.playSound(null, pos, BREATH, SoundSource.HOSTILE, finalVolume(2.0F,config.voiceVolumePercent), .9F);
                level.sendParticles(ParticleTypes.CLOUD, pos.getX()+.5, player.getEyeY(), pos.getZ()+.5, 22, .2, .2, .2, .01);
            }
            case "footsteps" -> {
                DreadState state=state(player);state.footstepTicks=400;state.footstepIndex=0;state.footstepAngle=player.getRandom().nextDouble()*Math.PI*2;
                double radius=44+player.getRandom().nextInt(15);
                state.footstepX=player.getX()+Math.cos(state.footstepAngle)*radius;state.footstepY=player.getY();state.footstepZ=player.getZ()+Math.sin(state.footstepAngle)*radius;
                state.footstepDx=(player.getX()-state.footstepX)/radius;state.footstepDz=(player.getZ()-state.footstepZ)/radius;
            }
            case "cold" -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY()+1, player.getZ(), 120, 3, 2, 3, .04);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, false, false));
                visual(player, VisualPacket.FLASH, 80, .35F, 0x55408090);
            }
            case "darkness" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120+stage*15, 0, false, false));
                state(player).heartbeatTicks=180; playFar(player, DRONE, 16, 30, .25F, .98F);
                visual(player, VisualPacket.BLINK, 35, .92F, 0xFF000000);
            }
            case "omen" -> {
                level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY()+1, player.getZ(), 80, 4, 2, 4, .03);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false));
                visual(player, VisualPacket.PULSE, 130, .65F, 0x88700020);
            }
            case "watcher" -> spawnWatchers(player, 1);
            case "watchers" -> spawnWatchers(player, Math.min(5, stage));
            case "falsecreeper" -> spawnHallucinations(player,HallucinationPacket.CREEPER,1);
            case "blink" -> visual(player, VisualPacket.BLINK, 22, .85F, 0xFF000000);
            case "flicker" -> visual(player, VisualPacket.FLASH, 100, .72F, 0xAA501018);
            case "fracture" -> visual(player, VisualPacket.FRACTURE, 220, .78F, 0x88500038);
            case "lurch" -> {
                player.push((player.getRandom().nextDouble()-.5)*1.1, .22, (player.getRandom().nextDouble()-.5)*1.1);
                player.hurtMarked = true; visual(player, VisualPacket.SHAKE, 55, .8F, 0x00000000);
            }
            case "phantomhit" -> {
                level.playSound(null, behind(player, 1), IMPACT, SoundSource.HOSTILE, 1.0F, .94F);
                visual(player, VisualPacket.FLASH, 50, .9F, 0xB0A00010);
                if (stage >= 6 && config.extremeStageCausesDamage) player.hurt(player.damageSources().magic(), 2.0F);
            }
            case "collapse" -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 260, 2, false, false));
                visual(player, VisualPacket.FRACTURE, 260, 1.0F, 0xCC600018);
                spawnWatchers(player, 5);
            }
            default -> { return false; }
        }
        return true;
    }

    private static void startDisturbance(ServerPlayer player, int kind, int seconds) {
        DreadState state=state(player);state.disturbanceTicks=(seconds+3)*20;state.disturbanceKind=kind;state.disturbanceAngle=player.getRandom().nextDouble()*Math.PI*2;
        double radius=38+player.getRandom().nextInt(19);
        state.disturbanceX=player.getX()+Math.cos(state.disturbanceAngle)*radius;state.disturbanceY=player.getY()+player.getRandom().nextInt(5)-2;state.disturbanceZ=player.getZ()+Math.sin(state.disturbanceAngle)*radius;
        player.serverLevel().playSound(null,BlockPos.containing(state.disturbanceX,state.disturbanceY,state.disturbanceZ),kind==1?KNOCK:SCRATCH,SoundSource.HOSTILE,finalVolume(kind==1?.18F:.15F,config.disturbanceVolumePercent),.98F+player.getRandom().nextFloat()*.04F);
    }

    private static void spawnWatchers(ServerPlayer player, int count) {
        spawnHallucinations(player,HallucinationPacket.WATCHER,count);
    }

    private static void spawnHallucinations(ServerPlayer player,int kind,int count) {
        if (!config.watcherSightings || !config.worldHallucinations) return;
        ServerLevel level=player.serverLevel();
        for(int i=0;i<count;i++) {
            double angle=player.getRandom().nextDouble()*Math.PI*2, distance=6+player.getRandom().nextInt(9);
            BlockPos ground=findGround(level,BlockPos.containing(player.getX()+Math.cos(angle)*distance,player.getY(),player.getZ()+Math.sin(angle)*distance));
            if(ground==null) continue;
            HallucinationPacket.spawn(player,kind,ground.getX()+.5,ground.getY(),ground.getZ()+.5,100+player.getRandom().nextInt(120));
        }
        if(count>1)playFar(player,IMPACT,8,16,.8F,.94F);
    }

    private static void tickSequences(ServerPlayer player,DreadState state) {
        ServerLevel level=player.serverLevel();
        if(state.footstepTicks>0 && (--state.footstepTicks==399 || state.footstepTicks==190)) {
            double travel=state.footstepIndex++*8.0;
            double x=state.footstepX+state.footstepDx*travel,z=state.footstepZ+state.footstepDz*travel;
            level.playSound(null,BlockPos.containing(x,state.footstepY,z),HAUNTED_FOOTSTEP,SoundSource.HOSTILE,finalVolume(.20F,config.footstepVolumePercent),.96F+player.getRandom().nextFloat()*.06F);
            level.sendParticles(ParticleTypes.SMOKE,x,state.footstepY+.1,z,2,.1,.04,.1,.004);
        }
        if(state.heartbeatTicks>0 && --state.heartbeatTicks%23==0) level.playSound(null,player.blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,1.5F,.62F);
        if(state.disturbanceTicks>0) {
            state.disturbanceTicks--;
            int interval=state.disturbanceKind==1?55:75;
            if(state.disturbanceTicks%interval==0){
            BlockPos pos=BlockPos.containing(state.disturbanceX,state.disturbanceY,state.disturbanceZ);
            level.playSound(null,pos,state.disturbanceKind==1?KNOCK:SCRATCH,SoundSource.HOSTILE,finalVolume(state.disturbanceKind==1?.17F:.14F,config.disturbanceVolumePercent),.97F+player.getRandom().nextFloat()*.05F);
            }
        }
    }

    private static void playFar(ServerPlayer player,SoundEvent sound,int min,int max,float volume,float pitch) {
        if(!config.customAudioEnabled || volume<=0)return;
        volume=masterVolume(volume);
        double scale=config.audioDistanceMultiplier/100.0;
        int low=Math.max(1,(int)(min*scale)), high=Math.max(low,(int)(max*scale));
        int distance=low+player.getRandom().nextInt(high-low+1);
        double angle=player.getRandom().nextDouble()*Math.PI*2;
        BlockPos pos=BlockPos.containing(player.getX()+Math.cos(angle)*distance,player.getY()+player.getRandom().nextInt(9)-4,player.getZ()+Math.sin(angle)*distance);
        player.serverLevel().playSound(null,pos,sound,SoundSource.HOSTILE,volume,pitch+player.getRandom().nextFloat()*.04F);
    }

    private static float familyVolume(float base,int percent){return base*percent/100F;}
    private static float masterVolume(float base){return config.customAudioEnabled?base*config.masterAudioVolumePercent/100F:0F;}
    private static float finalVolume(float base,int familyPercent){return masterVolume(familyVolume(base,familyPercent));}

    private static BlockPos behind(Player player,double distance) { double a=Math.toRadians(player.getYRot()+180); return BlockPos.containing(player.getX()-Math.sin(a)*distance,player.getEyeY(),player.getZ()+Math.cos(a)*distance); }
    private static BlockPos offset(Player player,int min,int max) { int d=min+player.getRandom().nextInt(Math.max(1,max-min+1)); double a=player.getRandom().nextDouble()*Math.PI*2; return BlockPos.containing(player.getX()+Math.cos(a)*d,player.getY()+player.getRandom().nextInt(7)-3,player.getZ()+Math.sin(a)*d); }
    private static BlockPos findGround(ServerLevel level,BlockPos start) { for(int dy=5;dy>=-8;dy--){BlockPos feet=start.offset(0,dy,0);if(level.getBlockState(feet.below()).isSolid()&&level.getBlockState(feet).isAir()&&level.getBlockState(feet.above()).isAir())return feet;}return null; }
    private static int awakeTicks(ServerPlayer player){ return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)); }
    private static int stageFor(int ticks){ double days=ticks/(double)DAY_TICKS;if(days<config.firstSymptomsAfterDays)return 0;if(days>=config.extremeAfterDays)return 6;double f=(days-config.firstSymptomsAfterDays)/(config.extremeAfterDays-config.firstSymptomsAfterDays);return Math.min(5,1+(int)(f*5)); }
    private static int nextCooldown(ServerPlayer player,int stage){
        int base=new int[]{999,180,150,110,80,55,35}[stage];
        // Rare clusters interrupt otherwise long, unpredictable quiet periods.
        if(stage>=3 && player.getRandom().nextInt(8)==0) return 7+player.getRandom().nextInt(12);
        int seconds=Math.max(12,(int)(base*(.55+player.getRandom().nextDouble()*1.15)));
        return Math.max(6,seconds*config.eventPacingPercent/100);
    }

    public static void addDeprivationHours(ServerPlayer player,int hours){ int value=Math.max(0,awakeTicks(player)+hours*1000);player.getStats().setValue(player,Stats.CUSTOM.get(Stats.TIME_SINCE_REST),value); }

    private static void registerCommands(){ CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(Commands.literal("deprived")
        .then(Commands.literal("status").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();int ticks=awakeTicks(p),stage=stageFor(ticks);ctx.getSource().sendSuccess(()->Component.literal(String.format("Awake %.1f days | deprivation stage %d/6",ticks/(double)DAY_TICKS,stage)),false);return stage;}))
        .then(Commands.literal("profile").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();SymptomProfile s=state(p).profile;ctx.getSource().sendSuccess(()->Component.literal(String.format("Symptom tendency: %s (audio %d, visual %d, paranoia %d, physical %d)",s.dominant(),s.auditory(),s.visual(),s.paranoia(),s.physical())),false);return 1;}))
        .then(Commands.literal("debug").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();DreadState s=state(p);s.debugHud=!s.debugHud;if(s.debugHud)DebugPacket.send(p,s,awakeTicks(p));else DebugPacket.disable(p);ctx.getSource().sendSuccess(()->Component.literal("Deprived developer HUD "+(s.debugHud?"enabled":"disabled")+"."),false);return s.debugHud?1:0;}))
        .then(Commands.literal("setdeprivation").requires(s->s.hasPermission(2)).then(Commands.argument("hours",IntegerArgumentType.integer(0,1000)).executes(ctx->{
            ServerPlayer p=ctx.getSource().getPlayerOrException(); int hours=IntegerArgumentType.getInteger(ctx,"hours"); int ticks=hours*1000;
            p.getStats().setValue(p,Stats.CUSTOM.get(Stats.TIME_SINCE_REST),ticks);
            DreadState state=state(p); state.stage=stageFor(ticks); state.lastRestTicks=ticks; state.pressureSeconds=0; state.cooldownTicks=0;
            VisualPacket.sync(p,config.intenseVisuals?state.stage:0);
            if(state.stage>0) { triggerRandomEvent(p,state.stage); state.cooldownTicks=nextCooldown(p,state.stage)*20; }
            return hours;
        })))
        .then(Commands.literal("trigger").requires(s->s.hasPermission(2)).then(Commands.argument("event",StringArgumentType.word()).suggests((ctx,b)->{for(String n:new String[]{"random","whisper","knocking","scratching","placement","drone","radio","singing","riser","deepdrone","cavein","breath","footsteps","cold","darkness","omen","watcher","watchers","falsecreeper","blink","phantomhit","collapse"})b.suggest(n);return b.buildFuture();}).executes(ctx->triggerEvent(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"event"))?1:0)))
        .then(Commands.literal("reload").requires(s->s.hasPermission(2)).executes(ctx->{config=DeprivedConfig.load();ctx.getSource().sendSuccess(()->Component.literal("Deprived configuration reloaded."),true);return 1;})))); }

    private static DreadState state(ServerPlayer player){return STATES.computeIfAbsent(player.getUUID(),DreadState::new);}
    public static DeprivedConfig config(){return config;}
    private static void visual(ServerPlayer player,int type,int duration,float strength,int color){
        if(!config.intenseVisuals || (type==VisualPacket.FLASH && !config.flashingEffects)) return;
        VisualPacket.send(player,type,duration,strength,color);
    }
    private static SoundEvent sound(String name){return SoundEvent.createFixedRangeEvent(id(name),96.0F);}
    private static void registerSound(String name,SoundEvent sound){Registry.register(BuiltInRegistries.SOUND_EVENT,id(name),sound);}
    public static ResourceLocation id(String path){return new ResourceLocation(MOD_ID,path);}
}
