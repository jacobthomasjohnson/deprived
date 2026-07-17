package dev.jacob.deprived.client;

import dev.jacob.deprived.DeprivedConfig;
import dev.jacob.deprived.DeprivedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class DeprivedConfigScreen extends Screen {
    private final Screen parent;
    private int page;

    public DeprivedConfigScreen(Screen parent){super(Component.literal("DEPRIVED // CONTROL ROOM"));this.parent=parent;}

    @Override protected void init(){
        clearWidgets();
        int center=width/2;
        addRenderableWidget(Button.builder(Component.literal("VISUAL"),b->{page=0;rebuildWidgets();}).bounds(center-154,28,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("SYSTEM"),b->{page=1;rebuildWidgets();}).bounds(center-50,28,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("DEVELOPER"),b->{page=2;rebuildWidgets();}).bounds(center+54,28,100,20).build());
        if(page==0)visualPage(); else if(page==1)systemPage(); else developerPage();
        addRenderableWidget(Button.builder(Component.literal("DONE"),b->onClose()).bounds(center-75,height-28,150,20).build());
    }

    @Override protected void rebuildWidgets(){init();}

    private void visualPage(){
        DeprivedConfig c=DeprivedMod.config();int left=width/2-205,right=width/2+5,y=62;
        toggle(left,y,"Analog visuals",()->c.analogVisuals,v->c.analogVisuals=v);toggle(right,y,"Film grain",()->c.filmGrain,v->c.filmGrain=v);y+=24;
        slider(left,y,"Analog intensity",()->c.analogEffectIntensity,v->c.analogEffectIntensity=v,0,100,"%");
        slider(right,y,"Grain opacity",()->c.grainMaximumOpacity,v->c.grainMaximumOpacity=v,0,30,"");y+=24;
        slider(left,y,"Moving edge noise",()->c.cornerShadowMaximumOpacity,v->c.cornerShadowMaximumOpacity=v,0,60,"");
        toggle(right,y,"Peripheral figures",()->c.peripheralHallucinations,v->c.peripheralHallucinations=v);y+=24;
        toggle(left,y,"Exposure breathing",()->c.exposureBreathing,v->c.exposureBreathing=v);
        toggle(right,y,"Rolling interference",()->c.rollingInterference,v->c.rollingInterference=v);y+=24;
        toggle(left,y,"Chromatic bleed",()->c.chromaticBleed,v->c.chromaticBleed=v);
        toggle(right,y,"Tracking slips",()->c.trackingSlips,v->c.trackingSlips=v);y+=24;
        toggle(left,y,"Film burns",()->c.filmBurns,v->c.filmBurns=v);
        toggle(right,y,"Phosphor trails",()->c.phosphorTrails,v->c.phosphorTrails=v);y+=24;
        toggle(left,y,"World hallucinations",()->c.worldHallucinations,v->c.worldHallucinations=v);
        toggle(right,y,"Flashing effects",()->c.flashingEffects,v->c.flashingEffects=v);y+=30;
        addRenderableWidget(Button.builder(Component.literal("PREVIEW ANALOG FAULT"),b->DeprivedClient.previewAnalog()).bounds(width/2-100,y,200,20).build());
    }

    private void systemPage(){
        DeprivedConfig c=DeprivedMod.config();int left=width/2-205,right=width/2+5,y=64;
        toggle(left,y,"Mod enabled",()->c.enabled,v->c.enabled=v);toggle(right,y,"Custom audio",()->c.customAudioEnabled,v->c.customAudioEnabled=v);y+=24;
        slider(left,y,"Event pacing",()->c.eventPacingPercent,v->c.eventPacingPercent=v,50,250,"%");slider(right,y,"Audio distance",()->c.audioDistanceMultiplier,v->c.audioDistanceMultiplier=v,50,250,"%");y+=24;
        slider(left,y,"Master audio",()->c.masterAudioVolumePercent,v->c.masterAudioVolumePercent=v,0,200,"%");slider(right,y,"Footsteps",()->c.footstepVolumePercent,v->c.footstepVolumePercent=v,0,200,"%");y+=24;
        slider(left,y,"Knocks / scratches",()->c.disturbanceVolumePercent,v->c.disturbanceVolumePercent=v,0,200,"%");slider(right,y,"Voices / breath",()->c.voiceVolumePercent,v->c.voiceVolumePercent=v,0,200,"%");y+=24;
        slider(left,y,"Ambience",()->c.ambienceVolumePercent,v->c.ambienceVolumePercent=v,0,200,"%");toggle(right,y,"Extreme damage",()->c.extremeStageCausesDamage,v->c.extremeStageCausesDamage=v);y+=24;
        slider(left,y,"First symptoms",()->c.firstSymptomsAfterDays,v->c.firstSymptomsAfterDays=v,0,3," days");slider(right,y,"Extreme stage",()->c.extremeAfterDays,v->c.extremeAfterDays=v,4,20," days");y+=32;
        addRenderableWidget(Button.builder(Component.literal("APPLY SERVER SETTINGS"),b->DeprivedClient.applyServerConfig()).bounds(width/2-100,y,200,20).build());
    }

    private void developerPage(){int x=width/2-205,y=66;
        command(x,y,"Random event","deprived trigger random");command(x+210,y,"Toggle monitor","deprived debug");y+=26;
        command(x,y,"Stage 1 — 24h","deprived setdeprivation 24");command(x+210,y,"Stage 3 — 82h","deprived setdeprivation 82");y+=26;
        command(x,y,"Stage 5 — 140h","deprived setdeprivation 140");command(x+210,y,"Stage 6 — 168h","deprived setdeprivation 168");y+=32;
        String[] events={"radio","singing","riser","deepdrone","footsteps","knocking","watcher","falsecreeper","blink","collapse"};
        for(int i=0;i<events.length;i++){int column=i%2,row=i/2;command(x+column*210,y+row*24,"Trigger "+events[i],"deprived trigger "+events[i]);}
    }

    private void command(int x,int y,String label,String command){addRenderableWidget(Button.builder(Component.literal(label),b->{if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.connection.sendCommand(command);}).bounds(x,y,200,20).build());}
    private void toggle(int x,int y,String label,BooleanSupplier get,Consumer<Boolean> set){addRenderableWidget(Button.builder(toggleText(label,get.getAsBoolean()),b->{set.accept(!get.getAsBoolean());b.setMessage(toggleText(label,get.getAsBoolean()));save();}).bounds(x,y,200,20).build());}
    private Component toggleText(String label,boolean value){return Component.literal(label+": "+(value?"ON":"OFF"));}
    private void slider(int x,int y,String label,IntSupplier get,IntConsumer set,int min,int max,String suffix){addRenderableWidget(new IntSlider(x,y,200,20,label,get,set,min,max,suffix));}
    private static void save(){DeprivedMod.config().sanitize();DeprivedMod.config().save();}

    @Override public void onClose(){save();Minecraft.getInstance().setScreen(parent);}
    @Override public void renderBackground(GuiGraphics graphics){}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        graphics.fill(width/2-220,14,width/2+220,height-36,0xD0141719);
        graphics.fill(width/2-220,14,width/2+220,16,0xAA51A59C);
        graphics.drawCenteredString(font,title,width/2,17,0xFFD7A5BD);
        graphics.drawCenteredString(font,page==0?"Changes preview and save immediately":page==1?"Server settings require permission":"Immediate test controls",width/2,51,0xFF9FC6BD);
        super.render(graphics,mouseX,mouseY,partialTick);
    }

    private static final class IntSlider extends AbstractSliderButton{
        private final String label,suffix;private final IntSupplier get;private final IntConsumer set;private final int min,max;
        IntSlider(int x,int y,int w,int h,String label,IntSupplier get,IntConsumer set,int min,int max,String suffix){super(x,y,w,h,Component.empty(),(get.getAsInt()-min)/(double)(max-min));this.label=label;this.get=get;this.set=set;this.min=min;this.max=max;this.suffix=suffix;updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(label+": "+get.getAsInt()+suffix));}
        @Override protected void applyValue(){set.accept(min+(int)Math.round(value*(max-min)));DeprivedConfigScreen.save();updateMessage();}
    }
}
