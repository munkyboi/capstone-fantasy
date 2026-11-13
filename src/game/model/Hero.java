package game.model;

/** A playable student with a class-specific special ability. */
public abstract class Hero extends Student {
    private static final long serialVersionUID = 1L;
    /** Creates a playable student.
     * @param id character index
     */
    protected Hero(int id) { super(id); }
    /** {@inheritDoc} */    @Override public abstract AbilityType getAbilityType();
    /** Mage specializing in magic. */
    public static final class Mage extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Zack. */ public Mage() { super(ZACK); }
        /** {@inheritDoc} */        @Override public AbilityType getAbilityType() { return AbilityType.ARCANE_BURST; }
    }
    /** Warrior specializing in sword attacks. */
    public static final class Warrior extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Bogart. */ public Warrior() { super(BOGART); }
        /** {@inheritDoc} */        @Override public AbilityType getAbilityType() { return AbilityType.CADET_STRIKE; }
    }
    /** Hunter specializing in multi-shot attacks. */
    public static final class Hunter extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Charice. */ public Hunter() { super(CHARICE); }
        /** {@inheritDoc} */        @Override public AbilityType getAbilityType() { return AbilityType.RAIN_OF_ARROWS; }
    }
    /** Monk specializing in healing. */
    public static final class Monk extends Hero {
        private static final long serialVersionUID = 1L;
        /** Creates Angel. */ public Monk() { super(ANGEL); }
        /** {@inheritDoc} */        @Override public AbilityType getAbilityType() { return AbilityType.HEALING_KISS; }
    }
}
