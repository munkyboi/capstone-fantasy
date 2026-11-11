package game.model;

/** A playable student with a class-specific special ability. */
public abstract class Hero extends Student {
    private static final long serialVersionUID = 1L;
    /** Creates a playable student.
     * @param id character index
     */
    protected Hero(int id) { super(id); }
    @Override public abstract AbilityType getAbilityType();
    /** Mage specializing in magic. */
    public static final class Mage extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Zack. */ public Mage() { super(ZACK); }
        @Override public AbilityType getAbilityType() { return AbilityType.ARCANE_BURST; }
    }
    /** Warrior specializing in sword attacks. */
    public static final class Warrior extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Bogart. */ public Warrior() { super(BOGART); }
        @Override public AbilityType getAbilityType() { return AbilityType.CADET_STRIKE; }
    }
    /** Hunter specializing in two attacks. */
    public static final class Hunter extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Charice. */ public Hunter() { super(CHARICE); }
        @Override public AbilityType getAbilityType() { return AbilityType.TWIN_SHOT; }
    }
    /** Monk specializing in healing. */
    public static final class Monk extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Angel. */ public Monk() { super(ANGEL); }
        @Override public AbilityType getAbilityType() { return AbilityType.HEALING_KISS; }
    }
}
