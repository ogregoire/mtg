package be.imgn.mtg.engine.characteristics;

/// Planeswalker subtypes, called planeswalker types ({@mtg.rule 205.3j}).
///
/// Planeswalker types are subtypes that are correlated to the planeswalker card type. Each
/// planeswalker type represents a specific character. A planeswalker typically has only one
/// planeswalker type.
public enum PlaneswalkerType implements Subtype {
    AJANI("Ajani"),
    AMINATOU("Aminatou"),
    ANGRATH("Angrath"),
    ARLINN("Arlinn"),
    ASHIOK("Ashiok"),
    BAHAMUT("Bahamut"),
    BASRI("Basri"),
    BOLAS("Bolas"),
    CALIX("Calix"),
    CHANDRA("Chandra"),
    COMET("Comet"),
    DACK("Dack"),
    DAKKON("Dakkon"),
    DARETTI("Daretti"),
    DAVRIEL("Davriel"),
    DIHADA("Dihada"),
    DOMRI("Domri"),
    DOVIN("Dovin"),
    ELLYWICK("Ellywick"),
    ELMINSTER("Elminster"),
    ELSPETH("Elspeth"),
    ESTRID("Estrid"),
    FREYALISE("Freyalise"),
    GARRUK("Garruk"),
    GIDEON("Gideon"),
    GRIST("Grist"),
    HUATLI("Huatli"),
    JACE("Jace"),
    JARED("Jared"),
    JAYA("Jaya"),
    JESKA("Jeska"),
    KAITO("Kaito"),
    KARN("Karn"),
    KASMINA("Kasmina"),
    KAYA("Kaya"),
    KIORA("Kiora"),
    KOTH("Koth"),
    LILIANA("Liliana"),
    LOLTH("Lolth"),
    LUKKA("Lukka"),
    MINSC("Minsc"),
    MORDENKAINEN("Mordenkainen"),
    NAHIRI("Nahiri"),
    NARSET("Narset"),
    NIKO("Niko"),
    NISSA("Nissa"),
    NIXILIS("Nixilis"),
    OKO("Oko"),
    RAL("Ral"),
    ROWAN("Rowan"),
    SAHEELI("Saheeli"),
    SAMUT("Samut"),
    SARKHAN("Sarkhan"),
    SERRA("Serra"),
    SIVITRI("Sivitri"),
    SORIN("Sorin"),
    SZAT("Szat"),
    TAMIYO("Tamiyo"),
    TASHA("Tasha"),
    TEFERI("Teferi"),
    TEYO("Teyo"),
    TEZZERET("Tezzeret"),
    TIBALT("Tibalt"),
    TYVAR("Tyvar"),
    UGIN("Ugin"),
    URZA("Urza"),
    VENSER("Venser"),
    VIVIEN("Vivien"),
    VRASKA("Vraska"),
    WILL("Will"),
    WINDGRACE("Windgrace"),
    WRENN("Wrenn"),
    XENAGOS("Xenagos"),
    YANGGU("Yanggu"),
    YANLING("Yanling"),
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
