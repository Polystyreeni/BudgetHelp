### Kuittailija
Nopea käyttöohje huonolle budjetointisovellukselle.


## Alkuvalikko
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/mainmenu.png?raw=true)
Alkuvalikko kertoo, kuinka paljon rahaa ostoksiin on kulunut tämän kuukauden ja edellisen kuukauden aikana. Lisäksi valikko sisältää luonnollisesti kaikki toiminnallisuudet, jotka sovelluksessa ovat mahdollisia.
1. Uusi kuitti: Valinnat uuden kuitin lisäykselle. Uuden kuitin voi luoda joko ottamalla siitä kuvan, tai kirjaamalla uuden kuitin tiedot käsin. Tästä valikosta voi myös jatkaa keskeneräistä kuittia, jos sellaisen on tallentanut. 
2. Ostotarkastelu: Tämä valikko sisältää kulutusyhteenvedon muodostuksen, sekä kuittihistorian tarkastelun.
3. Hallinta: Vähemmän hyödylliset ominaisuudet. Täältä voi tarkastella kaikkia sovelluksen tuotteita, tuotekategorioita, sekä sovelluksen yleisiä asetuksia. 


## Uusi kuitti kuvasta
Avaa laitteen kameran kuvan ottamista varten. Kuitti tulisi lukea mahdollisimman suorassa siten, että tuotteiden nimet ja hinnat ovat kutakuinkin samalla korkeudella. Kun kuva otetaan, sovellus pyrkii lukemaan kuvan perusteella tuote-hinta pareja, jotka voidaan siirtää uudelle kuitille. Suurella todennäköisyydellä kuitista luetaan myös virheellisiä tuotteita (esimerkiksi muuta tekstiä joka sisältää numeroita). Nämä on helpoin poistaa, kun tuotteet on lisätty uudelle kuitille (katso osio Uusi kuitti -> Tuotteen poistaminen).


## Uusi kuitti
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/new_receipt_list.png?raw=true)
1. Kuitin nimi (valinnainen): Vakioarvo tälle on kuitti + nykyinen päivämäärä
2. Kuitin päivämäärä (valinnainen): Kuitin päivämäärää käytetään kulutusyhteenvetojen muodostamisessa. Vakioarvo tälle on päivämäärä kuitin tallennushetkellä
3. Kuitin jatkaminen kuvan perusteella: Jos jotain tuotteita jäi lukematta, tästä napista pääsee takaisin kameranäkymään, jolloin kuitista voi ottaa uuden kuvan. Täydennyksessä kuitille lisätään vain sellaiset tuotteet, jotka eivät aiemmin esiintyneet kuitilla. 
4. Kuitille lisätty tuote: Tuotteella on nimi, kategoria sekä hinta.
5. Tuotteen muokkaus: Katso osio "Tuotteen lisäys / muokkaus"
6. Uuden tuotteen lisäys: Katso osio "Tuotteen lisäys / muokkaus"
7. Kuitin kokonaishinta lasketaan kuitilla olevien tuotteiden perusteella.
8. Kuitin tallennus tietokantaan. Kun kuitti on tallennettu, se näkyy kulutustarkasteluissa. Lisäksi tallennetaan kaikki kuitilla esiintyneet tuotteet, joita käytetään myöhempien kuittien tuotteiden kategorian ennakoimiseen.

# Tuotteen lisäys / muokkaus: 
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/add_new_product.png?raw=true)
1. Tuotteen nimi (pakollinen)
2. Tuotteen kategoria: Tästä valikosta voi valita myös **+** valinnan, jolloin voidaan luoda uusi kategoria.
3. Tuotteen hinta (pakollinen)
4. Lisää tuote nykyiselle kuitille / muuta tuotteelle uudet arvot

# Tuotteen poistaminen: 
Tuotteen voi poistaa pyyhkäisemällä tuotetta oikealle. 

# Kuitin tallennus keskeneräisenä
Kuitin voi halutessaan tallentaa keskeneräiseksi. Kuitti ja siihen kuuluvat tuotteet eivät tällöin kuulu kulutustarkasteluun. Keskeneräistä kuittia voidaan jatkaa joko kuvalla tai itse lisäämällä tuotteita. Jos uuden kuitin luonnin keskeyttää, käyttäjälle annetaan mahdollisuus tallentaa kuitti keskeneräisenä. Kuitti tallennetaan keskeneräisenä myös, jos käyttäjä jatkaa tuotteiden lisäämistä kuitille kameran kautta (Uusi kuitti, osio 3). Keskeneräiseen kuittiin pääsee myös alkuvalikosta, valitsemalla uusi kuitti -> kuitin jatkaminen.

**Huom! Vain yksi kuitti voi olla kerrallaan keskeneräisenä!**


## Kulutustarkastelu
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/spending_menu.png?raw=true)
Kulutustarkastelun ideana on kerätä annetulta aikaväliltä kaikki kuitit ja muodostaa niiden perusteella yhteenveto siitä, kuinka paljon kuhunkin tuotekategoriaan on kulunut rahaa.
1. Kulutustarkastelun alkuajankohta (kuukauden ensimmäinen päivä)
2. Kulutustarkastelun loppuajankohta (kuukauden ensimmäinen päivä)
3. Tarkastelun aikaväli. Vaihtoehtoina viikko ja kuukausi. 
4. Muodosta yhteenveto. 


## Kuittihistoria
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/receipt_history.png?raw=true)
Kuittihistorian tarkoituksena on nähdä kaikki sovellukseen tallennetut kuitit, sekä tarkempaa tietoa siitä, mitä nämä kuitit sisältävät.
1. Ilmoittaa kaikkien aikavälillä olevien kuittien kokonaissumman.
2. Kuittien rajaus päivämäärän perusteella. Kuitin on oltava muodostettu annetun päivän jälkeen.
3. Kuittien rajaus päivämäärän perusteella. Kuitin on oltava muodostettu ennen annettua päivää.
4. Rajatulle aikavälille kuuluva kuitti. Klikkaamalla kuittia saadaan lisätiedot kuitin tuotteista sekä mahdollisuus poistaa kuitti.

# Kuitin poistaminen
Kuitti voidaan poistaa menemällä kuitin lisätietoihin (yllä, osio 4) ja valitsemalla poista kuitti. Kuitin poistaminen ei poista sovelluksen tunnistamia tuotteita, mutta poisto hävittää kyseinen kuitin hinnan kulutustarkastelusta. 


## Tuotelista
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/product_list.png?raw=true)
Tuotelista näyttää kaikki tuotteet, jotka ovat esiintyneet jollakin sovellukseen lisätyllä kuitilla. Tuotteita käytetään määrittämään tuotekategorioita automaattisesti, kun uusia kuitteja luetaan. 
1. Valitse näytettävät kategoriat (yksi tai useampi)
2. Muokkaa tuotetta
3. Poista tuote tietokannasta

# Tuotteen poistaminen / muokkaaminen
Tuotteiden poistaminen / muokkaaminen ei vaikuta kuiteilla näkyviin hintoihin. 


## Kategorialista
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/category_list.png?raw=true)
Kategorialistan tarkoituksena on lisätä, muokata ja poistaa tuotteiden kategorioita.
1. Muokkaa kategorian nimeä.
2. Poista kategoria.

# Kategorian muokkaus ja poisto:
Kun kategorian nimi muutetaan, nimi muutetaan myös kaikkiin tuotteisiin, jotka kuuluvat tähän kategoriaan. 
Kun kategoria poistetaan, kaikki kategoriaan kuuluvat tuotteet siirretään "Määrittelemätön" kategoriaan.


## Asetukset
![alt text](https://github.com/Polystyreeni/BudgetHelp/blob/main/images/settings.png?raw=true)
Yllättävää kyllä asetuksista voi muokata sovelluksen asetuksia.
1. Käyttäjänimi. Tätä käytetään sovelluksen etusivulla, eikä ole väliä minkä nimen tähän laittaa. 
2. Valuutta. Ottaen huomioon, että koko sovellus on suomenkielinen, ei varmaan tarvitse vaihtaa eurosta mihinkään :)

**Kuitin lukuasetukset**
Nämä asetukset on tarkoitettu kameran lukutarkkuuden parantamiseen
3. **Maksimihinta:** Minkä hintainen yksittäinen tuote voi maksimissaan olla. Tällä pyritään estämään esimerkiksi se, että tuotteiden EAN-koodeja ei virheellisesti luokitella tuotteiden hinnaksi.
4. **Pystysuuntainen virhemarginaali:** Kuinka paljon pystysuuntaista heittoa tuotteen nimen ja hinnan välillä voi olla? Liian pieni arvo tuottaa huonoja tuloksia, sillä kuvat ovat harvoin täysin pystysuorassa. Toisaalta liian suuret arvot lisäävät virheellisten tuote-hinta parien määrää. 
5. **Ohitettavat sanat:** Lista sanoja, jotka tiedettävästi eivät ole ostettuja tuotteita. Joissain kuiteissa esiintyy tyypillisesti sanoja kuten "Yhteensä", "Maksutapahtuma" jne. Kuittia lukiessa sovellus tarkastaa, kuuluuko luetut tuotteet tähän ohitettavien sanojen listaan, ja jättää tällaiset tuotteet huomioimatta. Ohitettavia sanoja voi lisätä klikkaamalla. 

