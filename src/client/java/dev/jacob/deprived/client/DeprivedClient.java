package dev.jacob.deprived.client;

import dev.jacob.deprived.DeprivedMod;
import dev.jacob.deprived.DebugPacket;
import dev.jacob.deprived.ConfigPacket;
import dev.jacob.deprived.HallucinationPacket;
import dev.jacob.deprived.VisualPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/** Restrained peripheral hallucinations. Nothing here moves the camera or warps the world. */
public final class DeprivedClient implements ClientModInitializer {
    private static final Random RANDOM = new Random();
    private static long clientTicks;
    private static int stage;
    private static int effectType;
    private static int effectTicks;
    private static int effectDuration;
    private static float strength;
    private static int peripheralTicks;
    private static int peripheralSide;
    private static float peripheralHeight;
    private static int trackingTicks,trackingDuration,trackingY;
    private static int burnTicks,burnDuration,burnSide;
    private static int channelTicks;
    private static int nextEntityId=-10_000;
    private static final Map<Integer,ClientHallucination> HALLUCINATIONS=new HashMap<>();
    private static DebugState debug=DebugState.disabled();
    private static KeyMapping triggerEventKey;
    private static KeyMapping configMenuKey;

    @Override
    public void onInitializeClient() {
        triggerEventKey=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.deprived.trigger_event",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"category.deprived.developer"));
        configMenuKey=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.deprived.config_menu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F6,"category.deprived.developer"));
        ClientPlayNetworking.registerGlobalReceiver(VisualPacket.ID, (client, handler, buffer, responseSender) -> {
            int type=buffer.readVarInt(), duration=buffer.readVarInt();
            float amount=buffer.readFloat();
            buffer.readInt(); // legacy color field retained for packet compatibility
            client.execute(() -> receive(type,duration,amount));
        });
        ClientPlayNetworking.registerGlobalReceiver(HallucinationPacket.ID,(client,handler,buffer,responseSender)->{
            int kind=buffer.readVarInt();double x=buffer.readDouble(),y=buffer.readDouble(),z=buffer.readDouble();int duration=buffer.readVarInt();
            client.execute(()->receiveHallucination(client,kind,x,y,z,duration));
        });
        ClientPlayNetworking.registerGlobalReceiver(DebugPacket.ID,(client,handler,buffer,responseSender)->{
            boolean enabled=buffer.readBoolean();
            if(!enabled){client.execute(()->debug=DebugState.disabled());return;}
            DebugState next=new DebugState(true,buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readUtf(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readBoolean(),buffer.readBoolean(),buffer.readBoolean(),buffer.readUtf(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt());
            client.execute(()->debug=next);
        });
        ClientTickEvents.END_CLIENT_TICK.register(DeprivedClient::tick);
        HudRenderCallback.EVENT.register(DeprivedClient::render);
    }

    private static void receive(int type,int duration,float amount) {
        if(type==VisualPacket.SYNC) {
            stage=Math.round(amount);
            if(stage==0){effectTicks=peripheralTicks=0;effectType=0;clearHallucinations();}
            return;
        }
        effectType=type; effectTicks=effectDuration=Math.max(1,duration); strength=Math.max(0,Math.min(1,amount));
        if(type==VisualPacket.STATIC || type==VisualPacket.FRACTURE || type==VisualPacket.SHAKE || type==VisualPacket.FLASH) startPeripheral(Math.max(12,Math.min(45,duration/3)));
    }

    private static void tick(Minecraft client) {
        clientTicks++;
        while(configMenuKey.consumeClick())client.setScreen(new DeprivedConfigScreen(null));
        while(triggerEventKey.consumeClick()){
            if(client.player!=null && client.getConnection()!=null)client.player.connection.sendCommand("deprived trigger random");
        }
        tickHallucinations(client);
        if(effectTicks>0) effectTicks--;
        if(peripheralTicks>0) peripheralTicks--;
        if(trackingTicks>0)trackingTicks--;
        if(burnTicks>0)burnTicks--;
        if(channelTicks>0)channelTicks--;
        if(client.player==null || client.isPaused() || stage<2) return;
        // Long, irregular gaps are intentional. A shape is more effective if the player is unsure it happened.
        int averageGap=Math.max(900,4200-stage*480);
        if(DeprivedMod.config().peripheralHallucinations && peripheralTicks==0 && RANDOM.nextInt(averageGap)==0) startPeripheral(10+RANDOM.nextInt(14));
        if(DeprivedMod.config().analogVisuals){
            if(DeprivedMod.config().trackingSlips && stage>=3 && trackingTicks==0 && RANDOM.nextInt(Math.max(700,4400-stage*530))==0)startTracking();
            if(DeprivedMod.config().filmBurns && stage>=4 && burnTicks==0 && RANDOM.nextInt(Math.max(1300,7200-stage*760))==0)startBurn();
            if(stage>=5 && channelTicks==0 && RANDOM.nextInt(Math.max(1200,6200-stage*650))==0)channelTicks=18+RANDOM.nextInt(38);
        }
    }

    private static void startTracking(){trackingTicks=trackingDuration=18+RANDOM.nextInt(40);trackingY=12+RANDOM.nextInt(77);}
    private static void startBurn(){burnTicks=burnDuration=70+RANDOM.nextInt(100);burnSide=RANDOM.nextBoolean()?1:-1;}

    public static void previewAnalog(){trackingTicks=trackingDuration=90;trackingY=46;burnTicks=burnDuration=150;burnSide=RANDOM.nextBoolean()?1:-1;channelTicks=80;}
    public static void applyServerConfig(){if(ClientPlayNetworking.canSend(ConfigPacket.ID))ClientPlayNetworking.send(ConfigPacket.ID,ConfigPacket.create(DeprivedMod.config()));}

    private static void startPeripheral(int duration) {
        peripheralTicks=duration;
        peripheralSide=RANDOM.nextBoolean()?1:-1;
        peripheralHeight=.34F+RANDOM.nextFloat()*.28F;
    }

    private static void render(GuiGraphics graphics,float tickDelta) {
        if(stage==0){if(debug.enabled)drawDebug(graphics);return;}
        int width=graphics.guiWidth(),height=graphics.guiHeight();
        int ambientAlpha=Math.min(DeprivedMod.config().cornerShadowMaximumOpacity,Math.max(0,(stage-1)*7));
        drawEdgeNoise(graphics,width,height,ambientAlpha,tickDelta);
        if(DeprivedMod.config().filmGrain)drawGrain(graphics,width,height);
        if(DeprivedMod.config().analogVisuals){
            if(DeprivedMod.config().exposureBreathing)drawExposureBreathing(graphics,width,height,tickDelta);
            if(DeprivedMod.config().rollingInterference)drawRollingInterference(graphics,width,height,tickDelta);
            if(DeprivedMod.config().chromaticBleed)drawChromaticBleed(graphics,width,height,tickDelta);
            if(trackingTicks>0)drawTrackingSlip(graphics,width,height);
            if(burnTicks>0)drawFilmBurn(graphics,width,height);
            if(channelTicks>0)drawChannelLoss(graphics,width,height);
            if(DeprivedMod.config().phosphorTrails && stage>=3)drawPhosphorTrail(graphics,width,height,tickDelta);
        }

        if(effectTicks>0) {
            float life=effectTicks/(float)effectDuration;
            if(effectType==VisualPacket.BLINK) drawBlink(graphics,width,height,life,strength);
            else if(effectType==VisualPacket.PULSE || effectType==VisualPacket.FRACTURE) {
                int alpha=(int)((12+strength*28)*Math.sin((1-life)*Math.PI));
                drawEdgeNoise(graphics,width,height,Math.max(0,alpha),tickDelta);
            }
        }
        if(DeprivedMod.config().peripheralHallucinations && peripheralTicks>0) drawPeripheralFigure(graphics,width,height);
        if(debug.enabled)drawDebug(graphics);
    }

    private static void receiveHallucination(Minecraft client,int kind,double x,double y,double z,int duration) {
        if(kind==HallucinationPacket.CLEAR){clearHallucinations();return;}
        if(client.level==null || !DeprivedMod.config().worldHallucinations)return;
        Entity entity=switch(kind){
            case HallucinationPacket.CREEPER->EntityType.CREEPER.create(client.level);
            case HallucinationPacket.HUMAN->EntityType.ZOMBIE.create(client.level);
            default->EntityType.ENDERMAN.create(client.level);
        };
        if(entity==null)return;
        int id=nextEntityId--;
        entity.setId(id);entity.setPos(x,y,z);entity.setSilent(true);entity.setNoGravity(true);
        if(entity instanceof Mob mob)mob.setNoAi(true);
        if(client.player!=null){double dx=client.player.getX()-x,dz=client.player.getZ()-z;entity.setYRot((float)(Math.toDegrees(Math.atan2(-dx,dz))));}
        client.level.putNonPlayerEntity(id,entity);
        HALLUCINATIONS.put(id,new ClientHallucination(entity,duration));
    }

    private static void tickHallucinations(Minecraft client) {
        if(client.level==null){HALLUCINATIONS.clear();return;}
        Iterator<Map.Entry<Integer,ClientHallucination>> iterator=HALLUCINATIONS.entrySet().iterator();
        while(iterator.hasNext()){
            ClientHallucination hallucination=iterator.next().getValue();
            hallucination.ticks--;
            if(client.player!=null){
                Vec3 toward=hallucination.entity.position().add(0,hallucination.entity.getBbHeight()*.55,0).subtract(client.player.getEyePosition());
                double distance=toward.length();
                if(distance>0 && distance<32 && client.player.getViewVector(1).dot(toward.scale(1/distance))>.988)hallucination.ticks=Math.min(hallucination.ticks,4);
            }
            if(hallucination.ticks<=0 || hallucination.entity.isRemoved()){hallucination.entity.discard();iterator.remove();}
        }
    }

    private static void clearHallucinations(){for(ClientHallucination hallucination:HALLUCINATIONS.values())hallucination.entity.discard();HALLUCINATIONS.clear();}

    private static void drawGrain(GuiGraphics graphics,int width,int height){
        int maximum=DeprivedMod.config().grainMaximumOpacity;
        if(maximum<=0)return;
        int points=Math.max(stage*50,width*height*stage/18000);
        int alpha=Math.max(2,maximum*(stage+1)/7);
        for(int i=0;i<points;i++){
            int x=RANDOM.nextInt(Math.max(1,width)),y=RANDOM.nextInt(Math.max(1,height));
            int shade=RANDOM.nextBoolean()?0xD8D0D8:0x161016;
            int size=RANDOM.nextInt(9)==0?2:1;
            graphics.fill(x,y,Math.min(width,x+size),Math.min(height,y+size),((2+RANDOM.nextInt(Math.max(1,alpha-1)))<<24)|shade);
        }
        for(int i=0;i<stage;i++){
            int y=RANDOM.nextInt(Math.max(1,height)),x=RANDOM.nextInt(Math.max(1,width));
            int length=Math.max(8,width/(12+RANDOM.nextInt(18)));
            graphics.fill(x,y,Math.min(width,x+length),y+1,(Math.max(2,alpha/2)<<24)|0xB8AFB7);
        }
    }

    private static void drawColorDrift(GuiGraphics graphics,int width,int height){
        int alpha=stage-3;
        graphics.fill(0,0,Math.max(1,stage-3),height,(alpha<<24)|0x401040);
        graphics.fill(width-Math.max(1,stage-3),0,width,height,(alpha<<24)|0x104028);
    }

    private static float analogScale(){return DeprivedMod.config().analogEffectIntensity/100F;}

    private static void drawExposureBreathing(GuiGraphics graphics,int width,int height,float tickDelta){
        if(stage<2)return;
        double t=(clientTicks+tickDelta)*.011;
        double wave=.5+.5*Math.sin(t+Math.sin(t*.31)*1.7);
        int alpha=(int)((2+stage*1.8)*wave*analogScale());
        if(alpha>0)graphics.fill(0,0,width,height,(alpha<<24)|0xB5A6A0);
    }

    private static void drawRollingInterference(GuiGraphics graphics,int width,int height,float tickDelta){
        if(stage<2)return;
        int band=Math.max(8,height/22);
        int y=(int)(((clientTicks+tickDelta)*(.19+stage*.025))%(height+band))-band;
        int alpha=Math.max(2,(int)((stage+1)*2.2F*analogScale()));
        for(int i=0;i<band;i++){
            float fade=1-Math.abs(i-band*.5F)/(band*.5F);
            int a=(int)(alpha*Math.max(0,fade));
            if(a>0)graphics.fill(0,y+i,width,y+i+1,(a<<24)|0xC8C0BA);
        }
    }

    private static void drawChromaticBleed(GuiGraphics graphics,int width,int height,float tickDelta){
        if(stage<3)return;
        double breathe=.35+.65*(.5+.5*Math.sin((clientTicks+tickDelta)*.017));
        int alpha=(int)((stage-2)*5*breathe*analogScale());
        int depth=Math.max(1,(stage-2)*2);
        if(alpha<=0)return;
        graphics.fill(0,0,depth,height,(alpha<<24)|0xD02B58);
        graphics.fill(width-depth,0,width,height,(alpha<<24)|0x20B8B0);
        graphics.fill(0,0,width,1,(Math.max(1,alpha/2)<<24)|0x7560D8);
    }

    private static void drawTrackingSlip(GuiGraphics graphics,int width,int height){
        float life=trackingTicks/(float)Math.max(1,trackingDuration);
        float envelope=(float)Math.sin((1-life)*Math.PI);
        int y=height*trackingY/100;
        int h=Math.max(2,height/90+(int)(envelope*height/55));
        int alpha=(int)((8+stage*3)*envelope*analogScale());
        if(alpha<=0)return;
        graphics.fill(0,y,width,y+h,(alpha<<24)|0xC5BEC0);
        int tear=Math.max(4,(int)(width*.018*envelope));
        graphics.fill(0,y-1,tear,y+h+1,(Math.min(35,alpha+8)<<24)|0xB62D55);
        graphics.fill(width-tear,y+1,width,y+h+3,(Math.min(35,alpha+8)<<24)|0x2A9D9A);
        for(int i=0;i<3;i++)graphics.fill(0,y+h+2+i*2,width,y+h+3+i*2,(Math.max(1,alpha/3)<<24)|0x302A32);
    }

    private static void drawFilmBurn(GuiGraphics graphics,int width,int height){
        float life=burnTicks/(float)Math.max(1,burnDuration);
        float envelope=(float)Math.sin((1-life)*Math.PI);
        int depth=(int)(width*(.025+.075*envelope));
        for(int i=0;i<12;i++){
            float fade=1-i/12F;
            int alpha=(int)(18*envelope*fade*analogScale());
            if(alpha<=0)continue;
            int x1=burnSide<0?i*depth/12:width-(i+1)*depth/12;
            int x2=burnSide<0?(i+1)*depth/12:width-i*depth/12;
            int tint=i<3?0x8C2734:(i<7?0x9A5938:0x66445E);
            graphics.fill(x1,0,x2,height,(alpha<<24)|tint);
        }
    }

    private static void drawChannelLoss(GuiGraphics graphics,int width,int height){
        float envelope=(float)Math.sin(channelTicks/56F*Math.PI);
        int alpha=(int)((3+stage)*Math.abs(envelope)*analogScale());
        if(alpha>0)graphics.fill(0,0,width,height,(alpha<<24)|(channelTicks%2==0?0x446F68:0x79505E));
    }

    private static void drawPhosphorTrail(GuiGraphics graphics,int width,int height,float tickDelta){
        double pulse=.5+.5*Math.sin((clientTicks+tickDelta)*.07);
        int alpha=(int)((stage-2)*pulse*analogScale());
        if(alpha<=0)return;
        int cx=width/2,cy=height/2;
        graphics.fill(cx+3,cy,cx+5,cy+1,(alpha<<24)|0x42B9AD);
        graphics.fill(cx-5,cy,cx-3,cy+1,(alpha<<24)|0xB53C67);
    }

    private static void drawEdgeNoise(GuiGraphics graphics,int width,int height,int alpha,float tickDelta) {
        if(alpha<=0)return;
        int depth=Math.max(12,Math.min(width,height)/9);
        double time=(clientTicks+tickDelta)*.022;
        // Deterministic moving patches: smooth temporal motion without frame-to-frame static flicker.
        for(int i=0;i<112;i++) {
            long hash=mix(i*0x9E3779B97F4A7C15L);
            int side=(int)(hash&3);
            double phase=((hash>>>8)&0xFFFF)/65535.0*Math.PI*2;
            double speed=.18+((hash>>>24)&255)/255.0*.42;
            int edgeLength=side<2?height:width;
            double base=((hash>>>32)&0xFFFF)/65535.0*edgeLength;
            int along=(int)((base+(clientTicks+tickDelta)*speed)%Math.max(1,edgeLength));
            int inward=(int)(((hash>>>48)&255)/255.0*depth);
            int size=2+(int)((hash>>>56)&7);
            double breathe=.32+.68*(.5+.5*Math.sin(time+phase));
            double depthFade=Math.pow(1-inward/(double)depth,1.7);
            int patchAlpha=(int)(alpha*breathe*depthFade*(.45+((hash>>>16)&127)/254.0));
            if(patchAlpha<=0)continue;
            int x,y,w,h;
            if(side==0){x=inward;y=along;w=size;h=size*2;}
            else if(side==1){x=width-inward-size;y=along;w=size;h=size*2;}
            else if(side==2){x=along;y=inward;w=size*2;h=size;}
            else{x=along;y=height-inward-size;w=size*2;h=size;}
            int tint=(i%7==0&&stage>=4)?0x100810:0x050306;
            graphics.fill(Math.max(0,x),Math.max(0,y),Math.min(width,x+w),Math.min(height,y+h),(Math.min(255,patchAlpha)<<24)|tint);
        }
    }

    private static long mix(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return value^(value>>>31);}

    private static void drawBlink(GuiGraphics graphics,int width,int height,float life,float amount) {
        float closure=(float)Math.sin((1-life)*Math.PI);
        int lid=(int)(height*.22F*closure*amount);
        for(int i=0;i<6;i++) {
            int band=Math.max(1,lid/6);
            int alpha=35+i*28;
            graphics.fill(0,i*band,width,(i+1)*band,alpha<<24);
            graphics.fill(0,height-(i+1)*band,width,height-i*band,alpha<<24);
        }
    }

    private static void drawPeripheralFigure(GuiGraphics graphics,int width,int height) {
        float fade=Math.min(1,peripheralTicks/7.0F);
        int alpha=(int)((18+stage*7)*fade);
        int figureHeight=(int)(height*peripheralHeight);
        int bodyWidth=Math.max(3,figureHeight/8);
        int margin=Math.max(0,width/100);
        int x=peripheralSide<0?margin:width-margin-bodyWidth;
        int y=(int)(height*.48F-figureHeight*.42F);
        int color=Math.min(95,alpha)<<24;
        // An intentionally indistinct head-and-shoulders silhouette at the edge of vision.
        int head=Math.max(3,bodyWidth*2/3);
        graphics.fill(x+(bodyWidth-head)/2,y,x+(bodyWidth+head)/2,y+head,color);
        graphics.fill(x,y+head-1,x+bodyWidth,y+figureHeight,color);
        graphics.fill(peripheralSide<0?x:x-bodyWidth/2,y+head+2,peripheralSide<0?x+bodyWidth+bodyWidth/2:x+bodyWidth,y+figureHeight/2,color);
    }

    private static void drawDebug(GuiGraphics graphics){
        Minecraft client=Minecraft.getInstance();
        String context=(debug.underground?"underground ":"")+(debug.isolated?"isolated ":"")+(debug.storm?"storm":"");
        if(context.isBlank())context="open / accompanied / clear";
        String serverActive=debug.footsteps>0?"footsteps ":"";
        if(debug.disturbanceTicks>0)serverActive+=(debug.disturbanceKind==1?"knocking ":"scratching ");
        if(debug.heartbeat>0)serverActive+="heartbeat";
        if(serverActive.isBlank())serverActive="none";
        String localEffect=effectTicks>0?effectName(effectType)+" "+formatTicks(effectTicks):"none";
        String[] lines={
            "DEPRIVED  //  SIGNAL MONITOR",
            String.format("Awake  %.2f days    Stage  %d/6",debug.awakeTicks/24000.0,debug.stage),
            String.format("Profile  %s   A%d V%d P%d F%d",debug.profile,debug.audio,debug.visual,debug.paranoia,debug.physical),
            String.format("Context  light %d   %s",debug.light,context.trim()),
            String.format("Next event  %s",formatTicks(debug.cooldown)),
            String.format("Last  %s   %ds ago   pool %d",debug.lastEvent,debug.lastAge,debug.poolSize),
            "Server active  "+serverActive.trim(),
            String.format("Client  entities %d   peripheral %s",HALLUCINATIONS.size(),peripheralTicks>0?formatTicks(peripheralTicks):"none"),
            "Visual  "+localEffect,
            String.format("Analog %d%%   grain %d   edge %d",DeprivedMod.config().analogEffectIntensity,DeprivedMod.config().grainMaximumOpacity,DeprivedMod.config().cornerShadowMaximumOpacity),
            "Signal  "+analogState()
        };
        int width=0;for(String line:lines)width=Math.max(width,client.font.width(line));
        int x=6,y=6,lineHeight=10,padding=5;
        graphics.fill(x,y,x+width+padding*2,y+lines.length*lineHeight+padding*2,0xD0141719);
        graphics.fill(x,y,x+3,y+lines.length*lineHeight+padding*2,stageColor(debug.stage));
        graphics.fill(x+3,y,x+width+padding*2,y+1,0x8851A59C);
        for(int i=0;i<lines.length;i++)graphics.drawString(client.font,lines[i],x+padding+2,y+padding+i*lineHeight,i==0?0xFFD7A5BD:(i%3==0?0xFF9FC6BD:0xFFC4BBB8),false);
    }

    private static String analogState(){String value="stable";if(trackingTicks>0)value="tracking slip";if(burnTicks>0)value="film burn";if(channelTicks>0)value="channel drift";return value;}

    private static String effectName(int type){return switch(type){case VisualPacket.BLINK->"blink";case VisualPacket.FLASH->"peripheral";case VisualPacket.STATIC->"figure";case VisualPacket.PULSE->"shadow pulse";case VisualPacket.SHAKE->"presence";case VisualPacket.FRACTURE->"deep shadow";default->"none";};}
    private static String formatTicks(int ticks){return String.format("%.1fs",ticks/20.0);}
    private static int stageColor(int value){return switch(value){case 0->0xFF52675C;case 1,2->0xFF7A715D;case 3,4->0xFF75545C;default->0xFF6E283B;};}

    private static final class ClientHallucination {
        private final Entity entity;
        private int ticks;
        private ClientHallucination(Entity entity,int ticks){this.entity=entity;this.ticks=ticks;}
    }

    private record DebugState(boolean enabled,int awakeTicks,int stage,int cooldown,String profile,int audio,int visual,int paranoia,int physical,int light,boolean underground,boolean isolated,boolean storm,String lastEvent,int lastAge,int poolSize,int footsteps,int disturbanceKind,int disturbanceTicks,int heartbeat){
        private static DebugState disabled(){return new DebugState(false,0,0,0,"none",0,0,0,0,15,false,false,false,"none",0,0,0,0,0,0);}
    }
}
