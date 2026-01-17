package be.imgn.mtg.engine.characteristics;

/// Planeswalker subtypes, called planeswalker types ({@mtg.rule 205.3j}).
///
/// Planeswalker types are subtypes that are correlated to the planeswalker card type. Each
/// planeswalker type represents a specific character. A planeswalker typically has only one
/// planeswalker type.
public enum PlaneswalkerType implements Subtype {
    /// The Ajani planeswalker type.
    AJANI("Ajani"),
    /// The Aminatou planeswalker type.
    AMINATOU("Aminatou"),
    /// The Angrath planeswalker type.
    ANGRATH("Angrath"),
    /// The Arlinn planeswalker type.
    ARLINN("Arlinn"),
    /// The Ashiok planeswalker type.
    ASHIOK("Ashiok"),
    /// The Bahamut planeswalker type.
    BAHAMUT("Bahamut"),
    /// The Basri planeswalker type.
    BASRI("Basri"),
    /// The Bolas planeswalker type.
    BOLAS("Bolas"),
    /// The Calix planeswalker type.
    CALIX("Calix"),
    /// The Chandra planeswalker type.
    CHANDRA("Chandra"),
    /// The Comet planeswalker type.
    COMET("Comet"),
    /// The Dack planeswalker type.
    DACK("Dack"),
    /// The Dakkon planeswalker type.
    DAKKON("Dakkon"),
    /// The Daretti planeswalker type.
    DARETTI("Daretti"),
    /// The Davriel planeswalker type.
    DAVRIEL("Davriel"),
    /// The Dihada planeswalker type.
    DIHADA("Dihada"),
    /// The Domri planeswalker type.
    DOMRI("Domri"),
    /// The Dovin planeswalker type.
    DOVIN("Dovin"),
    /// The Ellywick planeswalker type.
    ELLYWICK("Ellywick"),
    /// The Elminster planeswalker type.
    ELMINSTER("Elminster"),
    /// The Elspeth planeswalker type.
    ELSPETH("Elspeth"),
    /// The Estrid planeswalker type.
    ESTRID("Estrid"),
    /// The Freyalise planeswalker type.
    FREYALISE("Freyalise"),
    /// The Garruk planeswalker type.
    GARRUK("Garruk"),
    /// The Gideon planeswalker type.
    GIDEON("Gideon"),
    /// The Grist planeswalker type.
    GRIST("Grist"),
    /// The Huatli planeswalker type.
    HUATLI("Huatli"),
    /// The Jace planeswalker type.
    JACE("Jace"),
    /// The Jared planeswalker type.
    JARED("Jared"),
    /// The Jaya planeswalker type.
    JAYA("Jaya"),
    /// The Jeska planeswalker type.
    JESKA("Jeska"),
    /// The Kaito planeswalker type.
    KAITO("Kaito"),
    /// The Karn planeswalker type.
    KARN("Karn"),
    /// The Kasmina planeswalker type.
    KASMINA("Kasmina"),
    /// The Kaya planeswalker type.
    KAYA("Kaya"),
    /// The Kiora planeswalker type.
    KIORA("Kiora"),
    /// The Koth planeswalker type.
    KOTH("Koth"),
    /// The Liliana planeswalker type.
    LILIANA("Liliana"),
    /// The Lolth planeswalker type.
    LOLTH("Lolth"),
    /// The Lukka planeswalker type.
    LUKKA("Lukka"),
    /// The Minsc planeswalker type.
    MINSC("Minsc"),
    /// The Mordenkainen planeswalker type.
    MORDENKAINEN("Mordenkainen"),
    /// The Nahiri planeswalker type.
    NAHIRI("Nahiri"),
    /// The Narset planeswalker type.
    NARSET("Narset"),
    /// The Niko planeswalker type.
    NIKO("Niko"),
    /// The Nissa planeswalker type.
    NISSA("Nissa"),
    /// The Nixilis planeswalker type.
    NIXILIS("Nixilis"),
    /// The Oko planeswalker type.
    OKO("Oko"),
    /// The Ral planeswalker type.
    RAL("Ral"),
    /// The Rowan planeswalker type.
    ROWAN("Rowan"),
    /// The Saheeli planeswalker type.
    SAHEELI("Saheeli"),
    /// The Samut planeswalker type.
    SAMUT("Samut"),
    /// The Sarkhan planeswalker type.
    SARKHAN("Sarkhan"),
    /// The Serra planeswalker type.
    SERRA("Serra"),
    /// The Sivitri planeswalker type.
    SIVITRI("Sivitri"),
    /// The Sorin planeswalker type.
    SORIN("Sorin"),
    /// The Szat planeswalker type.
    SZAT("Szat"),
    /// The Tamiyo planeswalker type.
    TAMIYO("Tamiyo"),
    /// The Tasha planeswalker type.
    TASHA("Tasha"),
    /// The Teferi planeswalker type.
    TEFERI("Teferi"),
    /// The Teyo planeswalker type.
    TEYO("Teyo"),
    /// The Tezzeret planeswalker type.
    TEZZERET("Tezzeret"),
    /// The Tibalt planeswalker type.
    TIBALT("Tibalt"),
    /// The Tyvar planeswalker type.
    TYVAR("Tyvar"),
    /// The Ugin planeswalker type.
    UGIN("Ugin"),
    /// The Urza planeswalker type.
    URZA("Urza"),
    /// The Venser planeswalker type.
    VENSER("Venser"),
    /// The Vivien planeswalker type.
    VIVIEN("Vivien"),
    /// The Vraska planeswalker type.
    VRASKA("Vraska"),
    /// The Will planeswalker type.
    WILL("Will"),
    /// The Windgrace planeswalker type.
    WINDGRACE("Windgrace"),
    /// The Wrenn planeswalker type.
    WRENN("Wrenn"),
    /// The Xenagos planeswalker type.
    XENAGOS("Xenagos"),
    /// The Yanggu planeswalker type.
    YANGGU("Yanggu"),
    /// The Yanling planeswalker type.
    YANLING("Yanling"),
    /// The Zariel planeswalker type.
    ZARIEL("Zariel");

    private final String text;

    PlaneswalkerType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
