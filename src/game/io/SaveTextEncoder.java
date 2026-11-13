package game.io;

import game.model.Equipment;
import game.model.GameProgress;
import game.model.Student;
import java.util.Properties;

/** Encodes the current camp state in the text save format. */
final class SaveTextEncoder {
    private SaveTextEncoder() {}
    static Properties encode(GameProgress checked) {
    Properties properties = new Properties();
    properties.setProperty("version", "9");
    properties.setProperty("mechanics.looperManaLeak", "true");
    properties.setProperty("party.count", Integer.toString(checked.getRoster().size()));
    properties.setProperty("party.active", checked.getParty().stream().map(s -> Integer.toString(s.getId())).collect(java.util.stream.Collectors.joining(",")));
    properties.setProperty("encounter", Integer.toString(checked.getEncounter()));
    properties.setProperty("market.merchantDefeated", Boolean.toString(checked.isMerchantDefeated()));
    properties.setProperty("inventory.petot", Integer.toString(checked.getPetot()));
    properties.setProperty("inventory.firstAidKits", Integer.toString(checked.getKits()));
    properties.setProperty("inventory.whiteFlowers", Integer.toString(checked.getInventory().getWhiteFlowers()));
    properties.setProperty("inventory.campusPass", "true");
    properties.setProperty("inventory.equipment", String.join(",", checked.getInventory().getEquipmentIds()));
    for (int stage = 0; stage < 5; stage++) {
        properties.setProperty("reward." + stage, Boolean.toString(checked.getInventory().isRewardClaimed(stage)));
    }
    for (int i = 0; i < checked.getRoster().size(); i++) {
        Student student = checked.getRoster().get(i);
        String prefix = "student." + (i + 1) + ".";
        properties.setProperty(prefix + "id", Integer.toString(student.getId()));
        properties.setProperty(prefix + "name", student.getName());
        properties.setProperty(prefix + "hp", Integer.toString(student.getHp()));
        properties.setProperty(prefix + "sp", Integer.toString(student.getSp()));
        properties.setProperty(prefix + "maxHp", Integer.toString(student.getMaxHp()));
        properties.setProperty(prefix + "level", Integer.toString(student.getLevel()));
        properties.setProperty(prefix + "experience", Long.toString(student.getExperience()));
        properties.setProperty(prefix + "growth", Integer.toString(student.getExperienceGrowth()));
        properties.setProperty(prefix + "levelCap", Integer.toString(student.getLevelCap()));
        for (Student.Stat stat : Student.Stat.values()) {
            properties.setProperty(prefix + "allocated." + stat.name(), Integer.toString(student.getAllocatedStatPoints(stat)));
        }
        for (Equipment.Slot gearSlot : Equipment.Slot.values()) {
            String id = student.getEquipmentId(gearSlot);
            properties.setProperty(prefix + gearSlot.name().toLowerCase(java.util.Locale.ROOT), id == null ? "" : id);
        }
    }
        return properties;
    }
}
