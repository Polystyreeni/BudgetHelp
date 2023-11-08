# Kuittailija
Sovellus kulutuksen tarkasteluun.

[Lataa sovellus](https://github.com/Polystyreeni/BudgetHelp/releases/)

[Käyttöohje](https://github.com/Polystyreeni/BudgetHelp/blob/main/user_manual.md)

## Ominaisuudet
- Lue kuittien sisältö kuvan perusteella
- Kategoroi tuotteet
- Tarkastele ja luo yhteenvetoja ostoksista ja näe mihin rahat uppoavat
- Ota vastaan kuittailua, kun kulutus ylittää aiemman kuukauden kulutuksen

## Käyttövaatimukset
- Android 6.0 tai uudempi
- Internet-yhteys (valinnainen viestipalveluun ja päivityksien tarkastukseen)


## Projektin kokoaminen
Valmis apk-tiedosto viimeisimmästä versiosta löytyy [täältä](https://github.com/Polystyreeni/BudgetHelp/releases/). Siinä epätodennäköisessä tapauksessa, että haluaisit koota tämän sovelluksen itse, nämä ohjeet auttavat alkuun.

**Ympäristö:**
Android Studio 2022.2.1

### Kokoamisohjeet
Kokoamisen pitäisi onnistua suoraan kloonamalla repositorio ja kokoamalla koodi Android Studiossa.

Omaan versioon on kuitenkin huomioitava seuraavat asiat:
1. URL-linkit pitää lisätä itse. Nämä voidaan asettaa tiedostosta `config/NetworkConfig.kt`.
  - `VERSION_URL` => Linkin pitäisi ladata tekstitiedosto, jossa versionumero muodossa `X.Y.Z` (esim 1.0.0)
  - `RELEASE_URL` => Linkki uusimpaan versioon. Kootussa sovelluksessa tämä on linkki GitHub repon Releases-osioon
  - `MESSAGE_URL` => Linkin pitäisi ladata tekstitiedosto, joka on muotoa `ID|VAADITTU_VERSIO|VIESTI` (esim abcd|1.0.0|Heippa maailma!)
  - `GUIDE_URL` => Linkki käyttöohjeeseen. Kootussa sovelluksessa GitHub repon user_manual.md
  Vaihtoehtoisesti nettiominaisuudet voidaan poistaa käytöstä asettamalla `ALLOW_NETWORK_ACCESS` arvoon false
2. Version päivitys tapahtuu muuttamalla `VersionManager.kt` tiedostosta muuttujan `currentVersion` arvo. Tätä verrataan `VERSION_URL` linkin takana olevaan versionumeroon. 
