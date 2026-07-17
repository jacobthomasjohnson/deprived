package dev.jacob.deprived;

import java.util.Random;
import java.util.UUID;

record SymptomProfile(int auditory, int visual, int paranoia, int physical) {
    static SymptomProfile from(UUID id) {
        Random random=new Random(id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),17));
        int a=1+random.nextInt(4),v=1+random.nextInt(4),p=1+random.nextInt(4),f=1+random.nextInt(4);
        // Every player has one pronounced tendency, but no symptom category is disabled.
        return switch(random.nextInt(4)) {
            case 0 -> new SymptomProfile(a+3,v,p,f);
            case 1 -> new SymptomProfile(a,v+3,p,f);
            case 2 -> new SymptomProfile(a,v,p+3,f);
            default -> new SymptomProfile(a,v,p,f+3);
        };
    }

    String dominant() {
        int max=Math.max(Math.max(auditory,visual),Math.max(paranoia,physical));
        if(auditory==max)return "auditory";
        if(visual==max)return "visual";
        if(paranoia==max)return "paranoia";
        return "physical";
    }
}
