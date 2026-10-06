"""The Ways' content: which OpenStreetMap relations draw each way, its stops, and what is said
about them, in English and Italian. Read by tools/build_ways.py, which writes the app's data
from it; this file is the one to edit.

A stop is a stage (a stamp in the credential, a notification when reached) or, with
stage=False, a place on the way worth a line (the sanctuary of San Luca, the Cisa Pass). Its
coordinates only find it on the line: the script snaps each one to the nearest point of the
way and measures its distance from there, and fails if a stop is more than MAX_SNAP_METRES off
the line or out of order.

Every note is one sentence, made to be read in a notification and heard, with nothing that
goes stale (no opening hours, no prices). Each was checked when written (1 Oct 2026; the fifth
way and Rome, Paris and Madrid on 2 Oct 2026, Lima and Cusco after them); the source is the
line after it. Berlin and Vienna (5 Oct 2026) were checked in the English and German Wikipedias,
New York in the English and Italian (or German), Rio in the English and Portuguese, Mexico City
and Buenos Aires in the English and Spanish, San Francisco in the English and Italian (or
Spanish, German), Québec in the English and French, Havana and Cartagena in the English and
Spanish, Tokyo in the English and Japanese (or Italian), Sydney in the English and German (or
Italian, French, Spanish), Seoul in the English and Korean, Beijing and Hong Kong in the English and Chinese,
Singapore in the English and Chinese (or German, Italian), Bangkok in the English and German (or
Italian), Kyoto in the English and Japanese, Hanoi in the English and French (or Italian), Melbourne in
the English and German (or French), Cairo in the English and French (or Arabic, German, Italian), Cape
Town in the English and German (or Dutch, French, Afrikaans), Marrakech in the English and French, Fez in
the English and French (or Italian, Spanish, German).
"""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Stop:
    key: str
    en: str
    it: str
    lat: float
    lon: float
    stage: bool = True
    note_en: str | None = None
    note_it: str | None = None


@dataclass(frozen=True)
class Way:
    id: str  # the Kotlin enum constant
    relations: list  # Waymarked Trails / OpenStreetMap relation ids, the way's main line
    name_en: str
    name_it: str
    route_en: str
    route_it: str
    country: str  # the locator map's country: IT, or IBERIA for Spain and Portugal
    stops: list = field(default_factory=list)
    # Where the relation stops short of the way's traditional end, the last stretch is joined
    # straight (a few hundred metres to a couple of kilometres inside a city).
    join_end: bool = False


VIA_DEGLI_DEI = Way(
    id="VIA_DEGLI_DEI",
    relations=[222322],
    name_en="Via degli Dei",
    name_it="Via degli Dei",
    route_en="Bologna to Florence, over the Apennines",
    route_it="Da Bologna a Firenze, attraverso l'Appennino",
    country="IT",
    join_end=True,
    stops=[
        Stop("bologna", "Bologna", "Bologna", 44.4938, 11.3426,
             note_en="The way begins in Piazza Maggiore, the heart of Bologna since the Middle Ages.",
             note_it="Il cammino parte da Piazza Maggiore, il cuore di Bologna dal Medioevo."),
        Stop("san_luca", "Sanctuary of San Luca", "Santuario di San Luca", 44.4794, 11.2981, stage=False,
             note_en="The climb to San Luca runs under a portico 3.8 km long, with 666 arches.",
             note_it="La salita a San Luca corre sotto un portico lungo 3,8 km, con 666 archi."),
        # Source: Wikipedia, Sanctuary of the Madonna di San Luca; UNESCO, The Porticoes of Bologna.
        Stop("badolo", "Badolo", "Badolo", 44.3667, 11.2917),
        Stop("monzuno", "Monzuno", "Monzuno", 44.2781, 11.2667),
        Stop("madonna_dei_fornelli", "Madonna dei Fornelli", "Madonna dei Fornelli", 44.2028, 11.2603,
             note_en="Beyond the village the way follows stretches of the Flaminia Militare, a road the Romans opened in 187 BC.",
             note_it="Oltre il paese il cammino segue tratti della Flaminia Militare, una strada aperta dai Romani nel 187 a.C."),
        # Source: Wikipedia (it), Flaminia militare.
        Stop("passo_della_futa", "Futa Pass", "Passo della Futa", 44.0911, 11.2817,
             note_en="At the Futa Pass, on the old Gothic Line, lies a German war cemetery of the Second World War.",
             note_it="Al Passo della Futa, sulla vecchia Linea Gotica, c'è un cimitero militare germanico della Seconda guerra mondiale."),
        # Source: Wikipedia, Futa Pass.
        Stop("san_piero_a_sieve", "San Piero a Sieve", "San Piero a Sieve", 43.9622, 11.3247,
             note_en="Above San Piero stands the fortress of San Martino, built by the Medici in the Mugello, their family's valley.",
             note_it="Sopra San Piero c'è la fortezza di San Martino, voluta dai Medici nel Mugello, la valle della loro famiglia."),
        # Source: Wikipedia (it), Fortezza di San Martino.
        Stop("bivigliano", "Bivigliano", "Bivigliano", 43.8983, 11.3236),
        Stop("fiesole", "Fiesole", "Fiesole", 43.8066, 11.2934,
             note_en="Fiesole is older than Florence: the Etruscans founded it, and its Roman theatre still holds summer shows.",
             note_it="Fiesole è più antica di Firenze: la fondarono gli Etruschi, e il suo teatro romano ospita ancora spettacoli d'estate."),
        # Source: Wikipedia, Fiesole.
        Stop("firenze", "Florence", "Firenze", 43.7696, 11.2558,
             note_en="The way ends in Piazza della Signoria, in front of Palazzo Vecchio.",
             note_it="Il cammino finisce in Piazza della Signoria, davanti a Palazzo Vecchio."),
    ],
)

CAMINO_PORTUGUES = Way(
    id="CAMINO_PORTUGUES",
    # The Portuguese Way's main relation runs from south of Porto; the line is its shortest path
    # from Porto's cathedral, where most pilgrims set out, to Santiago (owner, 2 Oct 2026).
    relations=[12786090],
    name_en="Camino Portugués",
    name_it="Cammino Portoghese",
    route_en="Porto to Santiago de Compostela, by Tui and Pontevedra",
    route_it="Da Porto a Santiago de Compostela, passando per Tui e Pontevedra",
    country="IBERIA",
    stops=[
        Stop("porto", "Porto", "Porto", 41.1428, -8.6112,
             note_en="Porto's old centre, from the cathedral down to the Douro, is a UNESCO World Heritage Site.",
             note_it="Il centro storico di Porto, dalla cattedrale fino al Douro, è patrimonio dell'umanità UNESCO."),
        # Source: Wikipedia, Porto (the UNESCO site, 1996).
        Stop("vilarinho", "Vilarinho", "Vilarinho", 41.3386, -8.6819),
        Stop("barcelos", "Barcelos", "Barcelos", 41.5315, -8.6192,
             note_en="In Barcelos, the legend goes, a roast cock stood up and crowed to save a pilgrim condemned to hang.",
             note_it="A Barcelos, dice la leggenda, un gallo arrosto si alzò e cantò per salvare un pellegrino condannato all'impiccagione."),
        # Source: Wikipedia, Rooster of Barcelos; Wikipedia (pt), Galo de Barcelos.
        Stop("ponte_de_lima", "Ponte de Lima", "Ponte de Lima", 41.7675, -8.5831,
             note_en="Ponte de Lima, granted its charter in 1125, is the oldest chartered town in Portugal.",
             note_it="Ponte de Lima, che ebbe il suo statuto nel 1125, è il più antico borgo del Portogallo con uno statuto."),
        # Source: Wikipedia, Ponte de Lima; Wikipedia (pt), Ponte de Lima.
        Stop("rubiaes", "Rubiães", "Rubiães", 41.8978, -8.6249),
        Stop("valenca", "Valença", "Valença", 42.0273, -8.6404, stage=False,
             note_en="Valença's walled fortress looks across the Minho at Tui: over the bridge, the way enters Spain.",
             note_it="La fortezza murata di Valença guarda Tui oltre il Minho: passato il ponte, il cammino entra in Spagna."),
        # Source: Wikipedia, Valença, Portugal; Wikipedia (es), Valença (Portugal).
        Stop("tui", "Tui", "Tui", 42.0459, -8.6444,
             note_en="Tui's cathedral, begun at the end of the 11th century, crowns a town that was long a frontier fortress.",
             note_it="La cattedrale di Tui, iniziata alla fine dell'XI secolo, domina una città che fu a lungo una fortezza di confine."),
        # Source: Wikipedia, Tui Cathedral; Wikipedia, Tui, Pontevedra.
        Stop("o_porrino", "O Porriño", "O Porriño", 42.1641, -8.6222),
        Stop("redondela", "Redondela", "Redondela", 42.2834, -8.6097,
             note_en="In 1702, in the strait of Rande below Redondela, an English and Dutch fleet attacked the Spanish treasure fleet.",
             note_it="Nel 1702, nello stretto di Rande sotto Redondela, una flotta inglese e olandese attaccò la flotta spagnola del tesoro."),
        # Source: Wikipedia, Battle of Vigo Bay; Wikipedia (es), Batalla de Rande.
        Stop("pontevedra", "Pontevedra", "Pontevedra", 42.4310, -8.6444,
             note_en="Pontevedra's chapel of the Pilgrim Virgin is built on the plan of a scallop shell, the pilgrims' sign.",
             note_it="A Pontevedra la cappella della Vergine Pellegrina ha la pianta di una conchiglia, il segno dei pellegrini."),
        # Source: Wikipedia, Pontevedra; Wikipedia (es), Iglesia de la Virgen Peregrina.
        Stop("caldas_de_reis", "Caldas de Reis", "Caldas de Reis", 42.6041, -8.6422,
             note_en="Caldas de Reis grew around hot springs that the Romans already used.",
             note_it="Caldas de Reis è cresciuta intorno a sorgenti calde che usavano già i Romani."),
        # Source: Wikipedia, Caldas de Reis; Wikipedia (es), Caldas de Reyes.
        Stop("padron", "Padrón", "Padrón", 42.7390, -8.6600,
             note_en="Here, the legend says, the boat bearing Saint James's body was moored to a stone, the pedrón.",
             note_it="Qui, dice la leggenda, la barca col corpo di san Giacomo fu legata a una pietra, il pedrón."),
        # Source: Wikipedia, Padrón.
        Stop("santiago", "Santiago de Compostela", "Santiago de Compostela", 42.8806, -8.5446,
             note_en="The way ends in the Praza do Obradoiro, before the cathedral of Santiago.",
             note_it="Il cammino finisce in Praza do Obradoiro, davanti alla cattedrale di Santiago."),
    ],
)

VIA_DI_FRANCESCO = Way(
    id="VIA_DI_FRANCESCO",
    relations=[6689530],
    name_en="Via di Francesco",
    name_it="Via di Francesco",
    route_en="La Verna to Rome, by way of Assisi",
    route_it="Da La Verna a Roma, passando per Assisi",
    country="IT",
    stops=[
        Stop("la_verna", "La Verna", "La Verna", 43.7072, 11.9306,
             note_en="Francis received the stigmata at La Verna in 1224, his first biographers wrote.",
             note_it="Francesco ricevette le stimmate alla Verna nel 1224, scrissero i suoi primi biografi."),
        # Source: Wikipedia, Sanctuary of La Verna.
        Stop("pieve_santo_stefano", "Pieve Santo Stefano", "Pieve Santo Stefano", 43.6703, 12.0406),
        Stop("sansepolcro", "Sansepolcro", "Sansepolcro", 43.5717, 12.1386,
             note_en="Sansepolcro is Piero della Francesca's town; his Resurrection hangs in its civic museum.",
             note_it="Sansepolcro è la città di Piero della Francesca; la sua Resurrezione è nel museo civico."),
        # Source: Wikipedia, The Resurrection (Piero della Francesca).
        Stop("citta_di_castello", "Città di Castello", "Città di Castello", 43.4573, 12.2405),
        Stop("pietralunga", "Pietralunga", "Pietralunga", 43.4425, 12.4356),
        Stop("gubbio", "Gubbio", "Gubbio", 43.3518, 12.5772,
             note_en="In Gubbio, the Fioretti tell, Francis tamed the wolf that frightened the town.",
             note_it="A Gubbio, raccontano i Fioretti, Francesco ammansì il lupo che spaventava la città."),
        # Source: Wikipedia, Wolf of Gubbio.
        Stop("valfabbrica", "Valfabbrica", "Valfabbrica", 43.1593, 12.6011),
        Stop("assisi", "Assisi", "Assisi", 43.0707, 12.6196,
             note_en="Assisi is Francis's town: his tomb lies beneath the basilica that bears his name.",
             note_it="Assisi è la città di Francesco: la sua tomba è sotto la basilica che porta il suo nome."),
        # Source: Wikipedia, Basilica of Saint Francis of Assisi.
        Stop("spello", "Spello", "Spello", 42.9893, 12.6719),
        Stop("foligno", "Foligno", "Foligno", 42.9561, 12.7033),
        Stop("trevi", "Trevi", "Trevi", 42.8770, 12.7476),
        Stop("spoleto", "Spoleto", "Spoleto", 42.7348, 12.7378,
             note_en="Spoleto's Ponte delle Torri, 230 metres long, began as a Roman aqueduct across the gorge.",
             note_it="Il Ponte delle Torri di Spoleto, lungo 230 metri, nacque come acquedotto romano sopra la gola."),
        # Source: Wikipedia, Ponte delle Torri.
        Stop("arrone", "Arrone", "Arrone", 42.5833, 12.7667),
        Stop("piediluco", "Piediluco", "Piediluco", 42.5353, 12.7498,
             note_en="Nearby, the Velino drops into the Marmore Falls, made by the Romans in 271 BC.",
             note_it="Qui vicino il Velino precipita nella Cascata delle Marmore, creata dai Romani nel 271 a.C."),
        # Source: Wikipedia, Cascata delle Marmore.
        Stop("poggio_bustone", "Poggio Bustone", "Poggio Bustone", 42.5014, 12.8858,
             note_en="Poggio Bustone is one of the four Franciscan sanctuaries of the Rieti valley, the Holy Valley.",
             note_it="Poggio Bustone è uno dei quattro santuari francescani della Valle Santa reatina."),
        # Source: Wikipedia (it), Valle Santa reatina.
        Stop("rieti", "Rieti", "Rieti", 42.4048, 12.8567,
             note_en="Rieti calls itself the centre of Italy: a stone in Piazza San Rufo marks the spot.",
             note_it="Rieti si dice il centro d'Italia: una pietra in Piazza San Rufo segna il punto."),
        # Source: Wikipedia, Rieti.
        Stop("poggio_san_lorenzo", "Poggio San Lorenzo", "Poggio San Lorenzo", 42.2525, 12.8433),
        Stop("ponticelli", "Ponticelli", "Ponticelli", 42.1914, 12.7969),
        Stop("montelibretti", "Montelibretti", "Montelibretti", 42.1356, 12.7375),
        Stop("monterotondo", "Monterotondo", "Monterotondo", 42.0528, 12.6175),
        Stop("monte_sacro", "Monte Sacro", "Monte Sacro", 41.9406, 12.5317),
        Stop("roma_san_pietro", "Rome", "Roma", 41.9022, 12.4568,
             note_en="The way ends in St Peter's Square, before the basilica.",
             note_it="Il cammino finisce in Piazza San Pietro, davanti alla basilica."),
    ],
)

CAMINO_FRANCES = Way(
    id="CAMINO_FRANCES",
    relations=[2163573],
    # Named as readers know it; the variant, the French Way (the classic one, and the most
    # walked), is said in the route line (owner, 1 Oct 2026).
    name_en="Camino de Santiago",
    name_it="Cammino di Santiago",
    route_en="The French Way, Saint-Jean-Pied-de-Port to Santiago de Compostela",
    route_it="Il Cammino Francese, da Saint-Jean-Pied-de-Port a Santiago de Compostela",
    country="IBERIA",
    stops=[
        Stop("saint_jean", "Saint-Jean-Pied-de-Port", "Saint-Jean-Pied-de-Port", 43.1631, -1.2376,
             note_en="Many pilgrims set out from here, at the foot of the Pyrenees.",
             note_it="Molti pellegrini partono da qui, ai piedi dei Pirenei."),
        Stop("roncesvalles", "Roncesvalles", "Roncisvalle", 43.0093, -1.3195,
             note_en="Here, in 778, Charlemagne's rearguard fell: the battle of the Song of Roland.",
             note_it="Qui, nel 778, cadde la retroguardia di Carlo Magno: la battaglia della Chanson de Roland."),
        # Source: Wikipedia, Battle of Roncevaux Pass.
        Stop("zubiri", "Zubiri", "Zubiri", 42.9306, -1.5036),
        Stop("pamplona", "Pamplona", "Pamplona", 42.8169, -1.6432,
             note_en="Every July, for San Fermín, Pamplona's bulls run through these streets.",
             note_it="Ogni luglio, per San Fermín, i tori di Pamplona corrono per queste strade."),
        # Source: Wikipedia, San Fermín.
        Stop("puente_la_reina", "Puente la Reina", "Puente la Reina", 42.6722, -1.8147,
             note_en="The town is named after its Romanesque bridge, built for pilgrims in the 11th century.",
             note_it="Il paese prende il nome dal ponte romanico costruito per i pellegrini nell'XI secolo."),
        # Source: Wikipedia, Puente la Reina.
        Stop("estella", "Estella", "Estella", 42.6714, -2.0306),
        Stop("los_arcos", "Los Arcos", "Los Arcos", 42.5694, -2.1917),
        Stop("logrono", "Logroño", "Logroño", 42.4650, -2.4456),
        Stop("najera", "Nájera", "Nájera", 42.4161, -2.7339),
        Stop("santo_domingo", "Santo Domingo de la Calzada", "Santo Domingo de la Calzada", 42.4408, -2.9539),
        Stop("belorado", "Belorado", "Belorado", 42.4206, -3.1903),
        Stop("san_juan_de_ortega", "San Juan de Ortega", "San Juan de Ortega", 42.3758, -3.4361),
        Stop("burgos", "Burgos", "Burgos", 42.3408, -3.7044,
             note_en="Burgos cathedral, begun in 1221, is a UNESCO World Heritage Site.",
             note_it="La cattedrale di Burgos, iniziata nel 1221, è patrimonio dell'umanità UNESCO."),
        # Source: Wikipedia, Burgos Cathedral.
        Stop("hornillos", "Hornillos del Camino", "Hornillos del Camino", 42.3383, -3.9253),
        Stop("castrojeriz", "Castrojeriz", "Castrojeriz", 42.2881, -4.1386),
        Stop("fromista", "Frómista", "Frómista", 42.2672, -4.4058),
        Stop("carrion", "Carrión de los Condes", "Carrión de los Condes", 42.3375, -4.6025),
        Stop("terradillos", "Terradillos de los Templarios", "Terradillos de los Templarios", 42.3628, -4.9208),
        Stop("el_burgo_ranero", "El Burgo Ranero", "El Burgo Ranero", 42.4228, -5.2208),
        Stop("mansilla", "Mansilla de las Mulas", "Mansilla de las Mulas", 42.4994, -5.4167),
        Stop("leon", "León", "León", 42.5987, -5.5671,
             note_en="León cathedral holds nearly 1,800 square metres of medieval stained glass.",
             note_it="La cattedrale di León custodisce quasi 1.800 metri quadrati di vetrate medievali."),
        # Source: Wikipedia, León Cathedral.
        Stop("hospital_de_orbigo", "Hospital de Órbigo", "Hospital de Órbigo", 42.4639, -5.8819),
        Stop("astorga", "Astorga", "Astorga", 42.4589, -6.0561,
             note_en="In Astorga stands the Episcopal Palace designed by Antoni Gaudí.",
             note_it="Ad Astorga c'è il Palazzo Episcopale progettato da Antoni Gaudí."),
        # Source: Wikipedia, Episcopal Palace of Astorga.
        Stop("rabanal", "Rabanal del Camino", "Rabanal del Camino", 42.4817, -6.2847),
        Stop("cruz_de_ferro", "Cruz de Ferro", "Cruz de Ferro", 42.4886, -6.3611, stage=False,
             note_en="At the iron cross, about 1,500 metres up, pilgrims leave a stone brought from home.",
             note_it="Alla croce di ferro, a circa 1.500 metri, i pellegrini lasciano una pietra portata da casa."),
        # Source: Wikipedia (es), Cruz de Ferro.
        Stop("ponferrada", "Ponferrada", "Ponferrada", 42.5461, -6.5908,
             note_en="Ponferrada's castle was held by the Knights Templar.",
             note_it="Il castello di Ponferrada appartenne ai Templari."),
        # Source: Wikipedia, Castle of the Templars, Ponferrada.
        Stop("villafranca", "Villafranca del Bierzo", "Villafranca del Bierzo", 42.6067, -6.8111),
        Stop("o_cebreiro", "O Cebreiro", "O Cebreiro", 42.7078, -7.0428,
             note_en="O Cebreiro, at the gate of Galicia, keeps its round stone houses with thatched roofs, the pallozas.",
             note_it="O Cebreiro, alla porta della Galizia, conserva le sue case tonde di pietra col tetto di paglia, le pallozas."),
        # Source: Wikipedia, O Cebreiro.
        Stop("triacastela", "Triacastela", "Triacastela", 42.7556, -7.2394),
        Stop("sarria", "Sarria", "Sarria", 42.7806, -7.4142,
             note_en="From Sarria, a little over 100 km from Santiago, the walk still earns the Compostela.",
             note_it="Da Sarria, poco più di 100 km da Santiago, il cammino vale ancora la Compostela."),
        # Source: Wikipedia, Compostela (certificate).
        Stop("portomarin", "Portomarín", "Portomarín", 42.8075, -7.6158),
        Stop("palas_de_rei", "Palas de Rei", "Palas de Rei", 42.8728, -7.8689),
        Stop("arzua", "Arzúa", "Arzúa", 42.9264, -8.1631),
        Stop("o_pedrouzo", "O Pedrouzo", "O Pedrouzo", 42.9050, -8.3633),
        Stop("santiago", "Santiago de Compostela", "Santiago de Compostela", 42.8806, -8.5446,
             note_en="The way ends in the Praza do Obradoiro, before the cathedral of Santiago.",
             note_it="Il cammino finisce in Praza do Obradoiro, davanti alla cattedrale di Santiago."),
    ],
)

VIA_FRANCIGENA = Way(
    id="VIA_FRANCIGENA",
    # Its Italian part, region by region (owner, 1 Oct 2026); the southern Via Francigena del
    # Sud (Campania, Puglia) and the variants are not in it.
    relations=[2458709, 1471008, 2452848, 1471005, 334891, 12554842, 6201614],
    name_en="Via Francigena",
    name_it="Via Francigena",
    route_en="The Great St Bernard Pass to Rome",
    route_it="Dal Gran San Bernardo a Roma",
    country="IT",
    stops=[
        Stop("gran_san_bernardo", "Great St Bernard Pass", "Colle del Gran San Bernardo", 45.8686, 7.1706,
             note_en="Travellers have found a hospice at this pass since about 1050, when Bernard of Aosta founded it.",
             note_it="Su questo colle i viaggiatori trovano un ospizio dal 1050 circa, quando lo fondò Bernardo d'Aosta."),
        # Source: Wikipedia, Great St Bernard Hospice.
        Stop("echevennoz", "Echevennoz", "Echevennoz", 45.8122, 7.2522),
        Stop("aosta", "Aosta", "Aosta", 45.7372, 7.3201,
             note_en="Aosta was Augusta Praetoria, a Roman town founded in 25 BC; its Arch of Augustus still stands.",
             note_it="Aosta era Augusta Praetoria, fondata dai Romani nel 25 a.C.; il suo Arco d'Augusto è ancora in piedi."),
        # Source: Wikipedia, Aosta.
        Stop("chatillon", "Châtillon", "Châtillon", 45.7500, 7.6167),
        Stop("verres", "Verrès", "Verrès", 45.6667, 7.6833),
        Stop("pont_saint_martin", "Pont-Saint-Martin", "Pont-Saint-Martin", 45.6000, 7.8000),
        Stop("ivrea", "Ivrea", "Ivrea", 45.4667, 7.8833,
             note_en="Ivrea's carnival is known for its battle of the oranges.",
             note_it="Il carnevale di Ivrea è famoso per la sua battaglia delle arance."),
        # Source: Wikipedia, Battle of the Oranges.
        Stop("viverone", "Viverone", "Viverone", 45.4272, 8.0497),
        Stop("santhia", "Santhià", "Santhià", 45.3667, 8.1667),
        Stop("vercelli", "Vercelli", "Vercelli", 45.3256, 8.4231,
             note_en="Vercelli keeps a 10th-century book of Old English poems, probably brought by English pilgrims.",
             note_it="Vercelli custodisce un libro di poesie in inglese antico del X secolo, forse portato da pellegrini inglesi."),
        # Source: Wikipedia, Vercelli Book.
        Stop("robbio", "Robbio", "Robbio", 45.2894, 8.5947),
        Stop("mortara", "Mortara", "Mortara", 45.2500, 8.7333),
        Stop("garlasco", "Garlasco", "Garlasco", 45.2000, 8.9167),
        Stop("pavia", "Pavia", "Pavia", 45.1847, 9.1582,
             note_en="Saint Augustine is buried in Pavia, in the church of San Pietro in Ciel d'Oro.",
             note_it="Sant'Agostino è sepolto a Pavia, nella chiesa di San Pietro in Ciel d'Oro."),
        # Source: Wikipedia, San Pietro in Ciel d'Oro.
        Stop("santa_cristina", "Santa Cristina e Bissone", "Santa Cristina e Bissone", 45.1561, 9.3994),
        Stop("orio_litta", "Orio Litta", "Orio Litta", 45.1606, 9.5603,
             note_en="Near here the way crosses the Po by boat, where Archbishop Sigeric crossed in 990.",
             note_it="Qui vicino il cammino attraversa il Po in barca, dove lo passò l'arcivescovo Sigerico nel 990."),
        # Source: Wikipedia (it), Guado di Sigerico.
        Stop("piacenza", "Piacenza", "Piacenza", 45.0522, 9.6930),
        Stop("fiorenzuola", "Fiorenzuola d'Arda", "Fiorenzuola d'Arda", 44.9264, 9.9128),
        Stop("fidenza", "Fidenza", "Fidenza", 44.8667, 10.0667),
        Stop("medesano", "Medesano", "Medesano", 44.7553, 10.1406),
        Stop("cassio", "Cassio", "Cassio", 44.5850, 10.0650),
        Stop("berceto", "Berceto", "Berceto", 44.5097, 10.0128),
        Stop("passo_della_cisa", "Cisa Pass", "Passo della Cisa", 44.4708, 9.9286, stage=False,
             note_en="At the Cisa Pass, 1,041 metres up, the way crosses the Apennines into Tuscany.",
             note_it="Al Passo della Cisa, a 1.041 metri, il cammino supera l'Appennino ed entra in Toscana."),
        # Source: Wikipedia, Cisa Pass.
        Stop("pontremoli", "Pontremoli", "Pontremoli", 44.3761, 9.8806),
        Stop("aulla", "Aulla", "Aulla", 44.2156, 9.9725),
        Stop("sarzana", "Sarzana", "Sarzana", 44.1131, 9.9603),
        Stop("massa", "Massa", "Massa", 44.0353, 10.1394),
        Stop("camaiore", "Camaiore", "Camaiore", 43.9431, 10.3022),
        Stop("lucca", "Lucca", "Lucca", 43.8429, 10.5027,
             note_en="Lucca's Renaissance walls, over four kilometres around, are a park on top.",
             note_it="Le mura rinascimentali di Lucca, lunghe più di quattro chilometri, in cima sono un parco."),
        # Source: Wikipedia, Walls of Lucca.
        Stop("altopascio", "Altopascio", "Altopascio", 43.8167, 10.6833),
        Stop("san_miniato", "San Miniato", "San Miniato", 43.6797, 10.8506),
        Stop("gambassi_terme", "Gambassi Terme", "Gambassi Terme", 43.5375, 10.9531),
        Stop("san_gimignano", "San Gimignano", "San Gimignano", 43.4677, 11.0430,
             note_en="San Gimignano still has 14 of its medieval towers, out of about 72.",
             note_it="San Gimignano ha ancora 14 delle sue torri medievali, su circa 72."),
        # Source: Wikipedia, San Gimignano.
        Stop("monteriggioni", "Monteriggioni", "Monteriggioni", 43.3897, 11.2236,
             note_en="In the Inferno, Dante likened Monteriggioni's ring of towers to giants.",
             note_it="Nell'Inferno, Dante paragonò la cerchia di torri di Monteriggioni a dei giganti."),
        # Source: Wikipedia, Monteriggioni.
        Stop("siena", "Siena", "Siena", 43.3188, 11.3308,
             note_en="Siena's Piazza del Campo holds the Palio twice each summer, on 2 July and 16 August.",
             note_it="Piazza del Campo a Siena ospita il Palio due volte ogni estate, il 2 luglio e il 16 agosto."),
        # Source: Wikipedia, Palio di Siena.
        Stop("ponte_d_arbia", "Ponte d'Arbia", "Ponte d'Arbia", 43.1903, 11.4639),
        Stop("san_quirico", "San Quirico d'Orcia", "San Quirico d'Orcia", 43.0581, 11.6050),
        Stop("radicofani", "Radicofani", "Radicofani", 42.8964, 11.7678,
             note_en="Radicofani's fortress was held by Ghino di Tacco, the gentleman bandit of the Decameron.",
             note_it="La rocca di Radicofani fu di Ghino di Tacco, il brigante gentiluomo del Decameron."),
        # Source: Wikipedia, Ghino di Tacco.
        Stop("acquapendente", "Acquapendente", "Acquapendente", 42.7428, 11.8650),
        Stop("bolsena", "Bolsena", "Bolsena", 42.6447, 11.9858,
             note_en="Bolsena lies on the largest volcanic lake in Europe.",
             note_it="Bolsena si affaccia sul lago vulcanico più grande d'Europa."),
        # Source: Wikipedia, Lake Bolsena.
        Stop("montefiascone", "Montefiascone", "Montefiascone", 42.5381, 12.0294),
        Stop("viterbo", "Viterbo", "Viterbo", 42.4207, 12.1077,
             note_en="In Viterbo, from 1268 to 1271, the cardinals held the longest papal election in history.",
             note_it="A Viterbo, dal 1268 al 1271, i cardinali tennero l'elezione papale più lunga della storia."),
        # Source: Wikipedia, 1268–1271 papal election.
        Stop("vetralla", "Vetralla", "Vetralla", 42.3203, 12.0539),
        Stop("sutri", "Sutri", "Sutri", 42.2453, 12.2153),
        Stop("campagnano", "Campagnano di Roma", "Campagnano di Roma", 42.1386, 12.3833),
        Stop("la_storta", "La Storta", "La Storta", 42.0083, 12.3653),
        Stop("roma_san_pietro", "Rome", "Roma", 41.9022, 12.4568,
             note_en="The way ends in St Peter's Square, before the basilica.",
             note_it="Il cammino finisce in Piazza San Pietro, davanti alla basilica."),
    ],
)

WAYS = [VIA_DEGLI_DEI, CAMINO_PORTUGUES, VIA_DI_FRANCESCO, CAMINO_FRANCES, VIA_FRANCIGENA]


@dataclass(frozen=True)
class Walk:
    """A walk through a city (Phase 11's second part), walked as an outing.

    Its line is routed once by BRouter (a walking router on OpenStreetMap data) through its
    places, in order, and saved in tools/walks/ by the script's `fetch`: the places are the
    route's waypoints, so each lies on the line. Behind it, the city's water and largest parks,
    from OpenStreetMap objects named here by id ("relation/28934"); canals are lines drawn
    [canal_width] metres wide. With [streets] (the default), the map also has the city's main
    streets, faint under the route, so the area can be recognised: fetched by tiles over what the
    page shows, and only the streets kept.
    """

    id: str
    city: str  # the city's key: walks of one city are listed together
    city_en: str
    city_it: str
    route_en: str
    route_it: str
    outing_en: str  # the outing's name in History and on Today
    outing_it: str
    country: str  # the locator map's country
    continent: str  # its group on the Ways page: a key of CONTINENTS
    stops: list = field(default_factory=list)
    water: list = field(default_factory=list)
    canals: list = field(default_factory=list)
    parks: list = field(default_factory=list)
    canal_width: float = 20.0
    streets: bool = True
    # Street classes drawn as minor streets beside the usual ones, for a city mapped mostly in
    # those (Cusco's old centre is residential lanes; so are Milan's and Rome's centres, where
    # tertiary streets are few).
    more_streets: tuple = ()
    # The shortest of those drawn, in metres (joined end to end): a dense grid of lanes kept
    # to its longer streets, so the map is as full as the other cities' and the arteries still
    # read. None: the same as every street.
    more_streets_min_metres: float | None = None
    # Water read from the street tiles (every closed way tagged as water in them) beside the
    # areas listed in [water]: for a city whose canals are hundreds of areas cut at every
    # bridge (Amsterdam's), too many to list by id. Only where the streets are fetched.
    water_from_tiles: bool = False
    # A city on the sea: its land is built from OpenStreetMap's coastline in its tiles, and its
    # map is cut out of the sea as a way's is (Rio's bay, New York's harbour).
    coast: bool = False


MILAN = Walk(
    id="MILAN_DUOMO_NAVIGLI",
    city="milan",
    city_en="Milan",
    city_it="Milano",
    route_en="From the Duomo to the Navigli, by the Castello",
    route_it="Dal Duomo ai Navigli, passando per il Castello",
    outing_en="A walk in Milan",
    outing_it="Passeggiata a Milano",
    country="IT",
    continent="EUROPE",
    water=["way/345030396"],
    canals=["relation/3340142", "relation/3350738"],
    parks=["relation/10172760", "way/5110320", "way/260829251"],
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    stops=[
        Stop("milan_duomo", "The Duomo", "Il Duomo", 45.4640, 9.1905,
             note_en="Milan began its cathedral in 1386, and building went on for nearly six centuries.",
             note_it="Milano iniziò il suo Duomo nel 1386, e il cantiere durò quasi sei secoli."),
        # Source: Wikipedia, Milan Cathedral.
        Stop("milan_galleria", "Galleria Vittorio Emanuele II", "Galleria Vittorio Emanuele II", 45.4656, 9.1900,
             note_en="Opened in 1867, it is one of the oldest covered shopping arcades in the world.",
             note_it="Inaugurata nel 1867, è una delle più antiche gallerie commerciali coperte del mondo."),
        # Source: Wikipedia, Galleria Vittorio Emanuele II.
        Stop("milan_scala", "La Scala", "Teatro alla Scala", 45.4676, 9.1891,
             note_en="La Scala opened in 1778; Verdi's Otello and Puccini's Turandot had their first nights here.",
             note_it="La Scala aprì nel 1778; qui debuttarono l'Otello di Verdi e la Turandot di Puccini."),
        # Source: Wikipedia, La Scala.
        Stop("milan_montenapoleone", "Via Montenapoleone", "Via Montenapoleone", 45.4678, 9.1958,
             note_en="The heart of the Quadrilatero della moda, the streets of Milan's fashion houses.",
             note_it="Il cuore del Quadrilatero della moda, le vie delle grandi case milanesi."),
        Stop("milan_brera", "Brera", "Brera", 45.4722, 9.1884,
             note_en="The Pinacoteca di Brera keeps Raphael's Marriage of the Virgin and Mantegna's Dead Christ.",
             note_it="La Pinacoteca di Brera custodisce lo Sposalizio della Vergine di Raffaello e il Cristo morto del Mantegna."),
        # Source: Wikipedia, Pinacoteca di Brera.
        Stop("milan_castello", "Sforza Castle", "Castello Sforzesco", 45.4703, 9.1781,
             note_en="The Sforza castle keeps Michelangelo's last sculpture, the Rondanini Pietà.",
             note_it="Il castello degli Sforza custodisce l'ultima scultura di Michelangelo, la Pietà Rondanini."),
        # Source: Wikipedia, Rondanini Pietà.
        Stop("milan_sempione", "Sempione Park", "Parco Sempione", 45.4730, 9.1770,
             note_en="The park was laid out in the 1890s on the castle's old parade ground.",
             note_it="Il parco fu disegnato negli anni Novanta dell'Ottocento sulla vecchia piazza d'armi del castello."),
        # Source: Wikipedia, Sempione Park.
        Stop("milan_arco_della_pace", "Arch of Peace", "Arco della Pace", 45.4757, 9.1724,
             note_en="Begun for Napoleon in 1807, the arch was finished in 1838 and dedicated to peace.",
             note_it="Iniziato per Napoleone nel 1807, l'arco fu finito nel 1838 e dedicato alla pace."),
        # Source: Wikipedia, Arco della Pace.
        Stop("milan_grazie", "Santa Maria delle Grazie", "Santa Maria delle Grazie", 45.4659, 9.1709,
             note_en="Leonardo painted the Last Supper on the wall of its refectory.",
             note_it="Leonardo dipinse il Cenacolo sulla parete del suo refettorio."),
        # Source: Wikipedia, The Last Supper (Leonardo).
        Stop("milan_sant_ambrogio", "Sant'Ambrogio", "Sant'Ambrogio", 45.4624, 9.1758,
             note_en="Saint Ambrose founded this basilica in the 4th century, and he is buried in it.",
             note_it="Sant'Ambrogio fondò questa basilica nel IV secolo, e qui è sepolto."),
        # Source: Wikipedia, Basilica of Sant'Ambrogio.
        Stop("milan_san_lorenzo", "Columns of San Lorenzo", "Colonne di San Lorenzo", 45.4582, 9.1810,
             note_en="Sixteen Roman columns, brought here in the 4th century from an older building.",
             note_it="Sedici colonne romane, portate qui nel IV secolo da un edificio più antico."),
        # Source: Wikipedia, Colonne di San Lorenzo.
        Stop("milan_porta_ticinese", "Porta Ticinese", "Porta Ticinese", 45.4538, 9.1808),
        Stop("milan_darsena", "The Darsena", "La Darsena", 45.4530, 9.1770,
             note_en="The Darsena was Milan's port, where the Navigli canals met.",
             note_it="La Darsena era il porto di Milano, dove si incontravano i Navigli."),
        Stop("milan_naviglio_grande", "Naviglio Grande", "Naviglio Grande", 45.4513, 9.1730,
             note_en="Begun in 1177, the canal brought the Duomo's marble into the city.",
             note_it="Iniziato nel 1177, il canale portava in città il marmo del Duomo."),
        # Source: Wikipedia, Naviglio Grande.
    ],
)

ROME = Walk(
    id="ROME_COLOSSEUM_VATICAN",
    city="rome",
    city_en="Rome",
    city_it="Roma",
    route_en="From the Colosseum to St Peter's, by the Pantheon",
    route_it="Dal Colosseo a San Pietro, passando per il Pantheon",
    outing_en="A walk in Rome",
    outing_it="Passeggiata a Roma",
    country="IT",
    continent="EUROPE",
    water=["relation/5071", "way/22797948", "way/22747533"],
    parks=["relation/2985896", "relation/11384819", "relation/10646138", "way/113038199"],
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    stops=[
        Stop("rome_colosseum", "The Colosseum", "Il Colosseo", 41.8909, 12.4919,
             note_en="Opened in AD 80, the Colosseum is the largest amphitheatre of the ancient world.",
             note_it="Inaugurato nell'80 d.C., il Colosseo è il più grande anfiteatro del mondo antico."),
        # Source: Wikipedia, Colosseum; Wikipedia (it), Colosseo.
        Stop("rome_arch_constantine", "Arch of Constantine", "Arco di Costantino", 41.8898, 12.4906,
             note_en="Raised in 315 for Constantine's victory at the Milvian Bridge, it reuses reliefs from older monuments.",
             note_it="Eretto nel 315 per la vittoria di Costantino a Ponte Milvio, riusa rilievi di monumenti più antichi."),
        # Source: Wikipedia, Arch of Constantine; Wikipedia (it), Arco di Costantino.
        Stop("rome_circus_maximus", "Circus Maximus", "Circo Massimo", 41.8862, 12.4853,
             note_en="The chariots raced here before crowds of some 150,000; the track is now a long meadow.",
             note_it="Qui correvano le bighe davanti a circa 150.000 spettatori; oggi la pista è un lungo prato."),
        # Source: Wikipedia, Circus Maximus.
        Stop("rome_mouth_of_truth", "Mouth of Truth", "Bocca della Verità", 41.8882, 12.4815,
             note_en="The old marble mask in the porch of Santa Maria in Cosmedin is said to bite the hand of a liar.",
             note_it="Il mascherone di marmo nel portico di Santa Maria in Cosmedin, si dice, morde la mano di chi mente."),
        # Source: Wikipedia, Bocca della Verità; Wikipedia (it), Bocca della Verità.
        Stop("rome_campidoglio", "Capitoline Hill", "Campidoglio", 41.8933, 12.4828,
             note_en="Michelangelo designed this square on the Capitoline Hill; its Palazzo Senatorio is still Rome's city hall.",
             note_it="Michelangelo progettò questa piazza sul Campidoglio; il suo Palazzo Senatorio è ancora il municipio di Roma."),
        # Source: Wikipedia, Piazza del Campidoglio; Wikipedia (it), Palazzo Senatorio.
        Stop("rome_vittoriano", "The Vittoriano", "Il Vittoriano", 41.8960, 12.4826,
             note_en="The white monument to Victor Emmanuel II, first king of united Italy, holds the Tomb of the Unknown Soldier.",
             note_it="Il monumento bianco a Vittorio Emanuele II, primo re dell'Italia unita, custodisce la tomba del Milite Ignoto."),
        # Source: Wikipedia, Victor Emmanuel II Monument; Wikipedia (it), Altare della Patria.
        Stop("rome_trevi", "Trevi Fountain", "Fontana di Trevi", 41.9010, 12.4833,
             note_en="Finished in 1762, the fountain is fed by the Aqua Virgo, an aqueduct the Romans opened in 19 BC.",
             note_it="Finita nel 1762, la fontana è alimentata dall'Acqua Vergine, un acquedotto aperto dai Romani nel 19 a.C."),
        # Source: Wikipedia, Trevi Fountain; Wikipedia (it), Fontana di Trevi.
        Stop("rome_spanish_steps", "Spanish Steps", "Scalinata di Trinità dei Monti", 41.9057, 12.4822,
             note_en="Opened for the Jubilee of 1725, the steps climb from Piazza di Spagna to the church of Trinità dei Monti.",
             note_it="Inaugurata per il Giubileo del 1725, la scalinata sale da Piazza di Spagna alla chiesa di Trinità dei Monti."),
        # Source: Wikipedia, Spanish Steps; Wikipedia (it), Scalinata di Trinità dei Monti
        # (they count the steps differently, 135 or 136, so the count is left out).
        Stop("rome_piazza_del_popolo", "Piazza del Popolo", "Piazza del Popolo", 41.9108, 12.4764,
             note_en="For centuries travellers from the north entered Rome here; the obelisk came from Egypt under Augustus.",
             note_it="Per secoli chi veniva dal nord entrava a Roma da qui; l'obelisco arrivò dall'Egitto con Augusto."),
        # Source: Wikipedia, Piazza del Popolo; Wikipedia (it), Obelisco Flaminio.
        Stop("rome_ara_pacis", "Ara Pacis", "Ara Pacis", 41.9062, 12.4755,
             note_en="Augustus's Altar of Peace was consecrated in 9 BC; it stands in a pavilion by Richard Meier, opened in 2006.",
             note_it="L'Altare della Pace di Augusto fu consacrato nel 9 a.C.; lo protegge un padiglione di Richard Meier, aperto nel 2006."),
        # Source: Wikipedia, Ara Pacis; Wikipedia (it), Ara Pacis.
        Stop("rome_pantheon", "The Pantheon", "Il Pantheon", 41.8992, 12.4768,
             note_en="Nearly two thousand years on, its dome is still the largest of unreinforced concrete in the world.",
             note_it="Dopo quasi duemila anni, la sua cupola è ancora la più grande del mondo in calcestruzzo non armato."),
        # Source: Wikipedia, Pantheon, Rome; Wikipedia (it), Pantheon (Roma).
        Stop("rome_navona", "Piazza Navona", "Piazza Navona", 41.8989, 12.4731,
             note_en="The square keeps the shape of Domitian's stadium; Bernini's Fountain of the Four Rivers stands in its middle.",
             note_it="La piazza ha la forma dello stadio di Domiziano; al centro c'è la Fontana dei Quattro Fiumi del Bernini."),
        # Source: Wikipedia, Piazza Navona; Wikipedia (it), Piazza Navona.
        Stop("rome_campo_de_fiori", "Campo de' Fiori", "Campo de' Fiori", 41.8956, 12.4722,
             note_en="The philosopher Giordano Bruno was burned here in 1600; his statue stands in the middle of the square.",
             note_it="Il filosofo Giordano Bruno fu arso qui nel 1600; la sua statua è al centro della piazza."),
        # Source: Wikipedia, Campo de' Fiori; Wikipedia (it), Monumento a Giordano Bruno.
        Stop("rome_castel_sant_angelo", "Castel Sant'Angelo", "Castel Sant'Angelo", 41.9025, 12.4665,
             note_en="Built as Hadrian's tomb, it became a fortress of the popes, joined to the Vatican by a raised passage.",
             note_it="Nato come tomba di Adriano, divenne una fortezza dei papi, unita al Vaticano da un passaggio sopraelevato."),
        # Source: Wikipedia, Castel Sant'Angelo; Wikipedia (it), Passetto di Borgo.
        Stop("rome_st_peters", "St Peter's Square", "Piazza San Pietro", 41.9022, 12.4574,
             note_en="Bernini's colonnade, four columns deep, reaches round the square like two open arms.",
             note_it="Il colonnato del Bernini, profondo quattro colonne, cinge la piazza come due braccia aperte."),
        # Source: Wikipedia, St. Peter's Square; Wikipedia (it), Piazza San Pietro.
    ],
)

PARIS = Walk(
    id="PARIS_VOSGES_EIFFEL",
    city="paris",
    city_en="Paris",
    city_it="Parigi",
    route_en="From Place des Vosges to the Eiffel Tower, by the Louvre",
    route_it="Da Place des Vosges alla Tour Eiffel, passando per il Louvre",
    outing_en="A walk in Paris",
    outing_it="Passeggiata a Parigi",
    country="FR",
    continent="EUROPE",
    water=["relation/2191006", "relation/10837211"],
    parks=["way/53820452", "way/4208595", "way/128206209"],
    stops=[
        Stop("paris_vosges", "Place des Vosges", "Place des Vosges", 48.8556, 2.3655,
             note_en="The oldest planned square in Paris, inaugurated in 1612; Victor Hugo lived at number 6.",
             note_it="La più antica piazza progettata di Parigi, inaugurata nel 1612; Victor Hugo abitò al numero 6."),
        # Source: Wikipedia, Place des Vosges; Wikipedia (fr), Place des Vosges.
        Stop("paris_hotel_de_ville", "Hôtel de Ville", "Hôtel de Ville", 48.8566, 2.3514,
             note_en="Paris has governed itself from this spot since 1357; the building was rebuilt after it burned in 1871.",
             note_it="Parigi si governa da questo luogo dal 1357; il palazzo fu ricostruito dopo l'incendio del 1871."),
        # Source: Wikipedia, Hôtel de Ville, Paris; Wikipedia (fr), Hôtel de ville de Paris.
        Stop("paris_notre_dame", "Notre-Dame", "Notre-Dame", 48.8533, 2.3488,
             note_en="Road distances from Paris are measured from point zero, a bronze star in the square before Notre-Dame.",
             note_it="Le distanze stradali da Parigi si misurano dal punto zero, una stella di bronzo sul sagrato di Notre-Dame."),
        # Source: Wikipedia (fr), Point zéro des routes de France.
        Stop("paris_sainte_chapelle", "Sainte-Chapelle", "Sainte-Chapelle", 48.8556, 2.3456,
             note_en="Louis IX built the chapel in the 1240s for the Crown of Thorns; its walls are almost all stained glass.",
             note_it="Luigi IX costruì la cappella negli anni Quaranta del Duecento per la Corona di Spine; le sue pareti sono quasi tutte vetrate."),
        # Source: Wikipedia, Sainte-Chapelle; Wikipedia (fr), Sainte-Chapelle.
        Stop("paris_pont_neuf", "Pont Neuf", "Pont Neuf", 48.8566, 2.3410,
             note_en="Despite its name, the New Bridge, finished in 1607, is the oldest bridge still standing on the Seine in Paris.",
             note_it="Nonostante il nome, il Ponte Nuovo, finito nel 1607, è il più antico ponte di Parigi ancora in piedi sulla Senna."),
        # Source: Wikipedia, Pont Neuf; Wikipedia (fr), Pont Neuf.
        Stop("paris_pont_des_arts", "Pont des Arts", "Pont des Arts", 48.8583, 2.3375,
             note_en="Built under Napoleon, it was the first iron bridge in Paris; it joins the Louvre to the Institut de France.",
             note_it="Costruito sotto Napoleone, fu il primo ponte di ferro di Parigi; unisce il Louvre all'Institut de France."),
        # Source: Wikipedia, Pont des Arts; Wikipedia (fr), Pont des Arts.
        Stop("paris_louvre", "The Louvre", "Il Louvre", 48.8613, 2.3346,
             note_en="A royal palace that became a museum in 1793; I. M. Pei's glass pyramid in its courtyard was finished in 1989.",
             note_it="Un palazzo reale diventato museo nel 1793; la piramide di vetro di I. M. Pei nel suo cortile fu finita nel 1989."),
        # Source: Wikipedia, Louvre; Wikipedia (fr), Musée du Louvre.
        Stop("paris_tuileries", "Tuileries Garden", "Giardino delle Tuileries", 48.8634, 2.3270,
             note_en="The palace burned in 1871, but its garden remains, as André Le Nôtre redesigned it in 1664.",
             note_it="Il palazzo bruciò nel 1871, ma il suo giardino è rimasto, come lo ridisegnò André Le Nôtre nel 1664."),
        # Source: Wikipedia, Tuileries Garden; Wikipedia (fr), Jardin des Tuileries.
        Stop("paris_concorde", "Place de la Concorde", "Place de la Concorde", 48.8656, 2.3212,
             note_en="Louis XVI was guillotined here in 1793; the obelisk, more than 3,000 years old, came from Luxor.",
             note_it="Qui, nel 1793, fu ghigliottinato Luigi XVI; l'obelisco, vecchio di oltre 3.000 anni, viene da Luxor."),
        # Source: Wikipedia, Place de la Concorde; Wikipedia (fr), Place de la Concorde.
        Stop("paris_grand_palais", "Grand Palais", "Grand Palais", 48.8645, 2.3135,
             note_en="It was built, with the Petit Palais across the avenue, for the Universal Exhibition of 1900.",
             note_it="Fu costruito, con il Petit Palais dall'altra parte del viale, per l'Esposizione universale del 1900."),
        # Source: Wikipedia, Grand Palais; Wikipedia (fr), Grand Palais (Paris).
        Stop("paris_champs_elysees", "Champs-Élysées", "Champs-Élysées", 48.8698, 2.3077,
             note_en="The avenue climbs almost two kilometres to the Arc de Triomphe; its name means the Elysian Fields.",
             note_it="Il viale sale per quasi due chilometri fino all'Arco di Trionfo; il suo nome significa Campi Elisi."),
        # Source: Wikipedia, Champs-Élysées.
        Stop("paris_arc_de_triomphe", "Arc de Triomphe", "Arco di Trionfo", 48.8732, 2.2963,
             note_en="Napoleon ordered the arch in 1806; the Unknown Soldier of the First World War has lain beneath it since 1921.",
             note_it="Napoleone ordinò l'arco nel 1806; sotto riposa dal 1921 il Milite Ignoto della Prima guerra mondiale."),
        # Source: Wikipedia, Arc de Triomphe; Wikipedia (fr), Arc de triomphe de l'Étoile.
        Stop("paris_trocadero", "Trocadéro", "Trocadéro", 48.8616, 2.2886,
             note_en="Here, at the Palais de Chaillot, the Universal Declaration of Human Rights was adopted on 10 December 1948.",
             note_it="Qui, al Palais de Chaillot, il 10 dicembre 1948 fu adottata la Dichiarazione universale dei diritti umani."),
        # Source: Wikipedia, Palais de Chaillot; Wikipedia (fr), Palais de Chaillot.
        Stop("paris_eiffel", "Eiffel Tower", "Tour Eiffel", 48.8582, 2.2945,
             note_en="Built for the Universal Exhibition of 1889, the tower was meant to stand for only twenty years.",
             note_it="Costruita per l'Esposizione universale del 1889, la torre doveva restare in piedi solo vent'anni."),
        # Source: Wikipedia, Eiffel Tower; Wikipedia (fr), Tour Eiffel.
    ],
)

LONDON = Walk(
    id="LONDON_PALACE_TOWER",
    city="london",
    city_en="London",
    city_it="Londra",
    route_en="From Buckingham Palace to Tower Bridge, along the Thames",
    route_it="Da Buckingham Palace al Tower Bridge, lungo il Tamigi",
    outing_en="A walk in London",
    outing_it="Passeggiata a Londra",
    country="GB",
    continent="EUROPE",
    water=["relation/28934", "relation/70347", "relation/276130"],
    parks=["way/374960368", "way/863554956", "way/4373996", "way/4254099", "way/367694522", "way/142680571",
           "way/372975520"],
    stops=[
        Stop("london_buckingham", "Buckingham Palace", "Buckingham Palace", 51.5008, -0.1430,
             note_en="The monarch's London home since Queen Victoria moved in, in 1837.",
             note_it="La casa londinese del sovrano da quando vi si trasferì la regina Vittoria, nel 1837."),
        # Source: Wikipedia, Buckingham Palace.
        Stop("london_st_james_park", "St James's Park", "St James's Park", 51.5031, -0.1332,
             note_en="Pelicans have lived here since a Russian ambassador gave them to Charles II in 1664.",
             note_it="Qui vivono pellicani da quando un ambasciatore russo li regalò a Carlo II, nel 1664."),
        # Source: Wikipedia, St James's Park.
        Stop("london_horse_guards", "Horse Guards Parade", "Horse Guards Parade", 51.5047, -0.1283,
             note_en="Every June the King's birthday parade, Trooping the Colour, is held here.",
             note_it="Ogni giugno qui si tiene la parata per il compleanno del re, il Trooping the Colour."),
        # Source: Wikipedia, Trooping the Colour.
        Stop("london_trafalgar", "Trafalgar Square", "Trafalgar Square", 51.5085, -0.1284,
             note_en="Nelson's Column, about 52 metres tall, remembers the battle of Trafalgar of 1805.",
             note_it="La colonna di Nelson, alta circa 52 metri, ricorda la battaglia di Trafalgar del 1805."),
        # Source: Wikipedia, Nelson's Column.
        Stop("london_downing", "Downing Street", "Downing Street", 51.5034, -0.12645,
             note_en="Number 10 has been the Prime Minister's house since Robert Walpole moved in, in 1735.",
             note_it="Il numero 10 è la casa del primo ministro da quando vi entrò Robert Walpole, nel 1735."),
        # Source: Wikipedia, 10 Downing Street.
        Stop("london_abbey", "Westminster Abbey", "Abbazia di Westminster", 51.49936, -0.12905,
             note_en="Almost every English and British monarch since 1066 has been crowned here.",
             note_it="Quasi tutti i sovrani inglesi e britannici dal 1066 sono stati incoronati qui."),
        # Source: Wikipedia, Westminster Abbey.
        Stop("london_parliament", "Houses of Parliament", "Palazzo di Westminster", 51.50055, -0.12585,
             note_en="Big Ben is the great bell inside the clock tower, named Elizabeth Tower in 2012.",
             note_it="Big Ben è la grande campana dentro la torre dell'orologio, chiamata Elizabeth Tower dal 2012."),
        # Source: Wikipedia, Big Ben.
        Stop("london_westminster_bridge", "Westminster Bridge", "Westminster Bridge", 51.50085, -0.12178,
             note_en="Wordsworth wrote a sonnet on this bridge in 1802: earth has not anything to show more fair.",
             note_it="Wordsworth scrisse un sonetto su questo ponte nel 1802: la terra non ha nulla di più bello."),
        # Source: Wikipedia, Composed upon Westminster Bridge, September 3, 1802.
        Stop("london_eye", "London Eye", "London Eye", 51.5033, -0.1196,
             note_en="The wheel, 135 metres tall, opened in 2000.",
             note_it="La ruota, alta 135 metri, fu inaugurata nel 2000."),
        # Source: Wikipedia, London Eye.
        Stop("london_festival_hall", "Royal Festival Hall", "Royal Festival Hall", 51.5058, -0.1168,
             note_en="It was built for the Festival of Britain, in 1951.",
             note_it="Fu costruita per il Festival of Britain, nel 1951."),
        # Source: Wikipedia, Royal Festival Hall.
        Stop("london_tate", "Tate Modern", "Tate Modern", 51.5074, -0.0993,
             note_en="A power station until 1981, it opened as a gallery of modern art in 2000.",
             note_it="Centrale elettrica fino al 1981, aprì come museo d'arte moderna nel 2000."),
        # Source: Wikipedia, Tate Modern.
        Stop("london_globe", "Shakespeare's Globe", "Globe di Shakespeare", 51.5081, -0.0972,
             note_en="A rebuilding of Shakespeare's theatre, opened in 1997 near the site of the first.",
             note_it="Una ricostruzione del teatro di Shakespeare, aperta nel 1997 vicino a dove sorgeva il primo."),
        # Source: Wikipedia, Shakespeare's Globe.
        Stop("london_millennium_bridge", "Millennium Bridge", "Millennium Bridge", 51.5099, -0.0985,
             note_en="Opened in June 2000, it swayed under the walkers' feet and was closed two days later.",
             note_it="Aperto nel giugno 2000, oscillava sotto i passi della gente e fu chiuso due giorni dopo."),
        # Source: Wikipedia, Millennium Bridge, London.
        Stop("london_st_pauls", "St Paul's Cathedral", "Cattedrale di St Paul", 51.5138, -0.0985,
             note_en="Christopher Wren built the cathedral after the Great Fire of 1666.",
             note_it="Christopher Wren costruì la cattedrale dopo il grande incendio del 1666."),
        # Source: Wikipedia, St Paul's Cathedral.
        Stop("london_monument", "The Monument", "The Monument", 51.5101, -0.0859,
             note_en="Its 61 metres are its distance from the bakery in Pudding Lane where the Great Fire began.",
             note_it="I suoi 61 metri sono la distanza dal forno di Pudding Lane dove iniziò il grande incendio."),
        # Source: Wikipedia, Monument to the Great Fire of London.
        Stop("london_tower", "Tower of London", "Torre di Londra", 51.5082, -0.0762,
             note_en="Founded by William the Conqueror, it keeps the Crown Jewels.",
             note_it="Fondata da Guglielmo il Conquistatore, custodisce i gioielli della Corona."),
        # Source: Wikipedia, Tower of London.
        Stop("london_tower_bridge", "Tower Bridge", "Tower Bridge", 51.5055, -0.0754,
             note_en="Its two halves still lift to let tall ships through, as they have since 1894.",
             note_it="Le sue due metà si alzano ancora per far passare le navi alte, come dal 1894."),
        # Source: Wikipedia, Tower Bridge.
    ],
)


MADRID = Walk(
    id="MADRID_DEBOD_RETIRO",
    city="madrid",
    city_en="Madrid",
    city_it="Madrid",
    route_en="From the Temple of Debod to the Retiro, by the Prado",
    route_it="Dal Tempio di Debod al Retiro, passando per il Prado",
    outing_en="A walk in Madrid",
    outing_it="Passeggiata a Madrid",
    country="IBERIA",
    continent="EUROPE",
    water=["relation/3615910", "way/4088758"],
    parks=["relation/13616929", "relation/535694", "relation/1505193", "relation/2061818", "way/15244804"],
    stops=[
        Stop("madrid_debod", "Temple of Debod", "Tempio di Debod", 40.4240, -3.7176,
             note_en="An Egyptian temple of the 2nd century BC, given to Spain for its help in saving the monuments of Nubia.",
             note_it="Un tempio egizio del II secolo a.C., donato alla Spagna per l'aiuto nel salvare i monumenti della Nubia."),
        # Source: Wikipedia, Temple of Debod; Wikipedia (es), Templo de Debod.
        Stop("madrid_plaza_de_espana", "Plaza de España", "Plaza de España", 40.4233, -3.7123,
             note_en="Don Quixote and Sancho Panza ride in bronze at the foot of the monument to Cervantes.",
             note_it="Don Chisciotte e Sancho Panza cavalcano in bronzo ai piedi del monumento a Cervantes."),
        # Source: Wikipedia, Plaza de España, Madrid; Wikipedia (es), Monumento a Cervantes (Madrid).
        Stop("madrid_palacio_real", "Royal Palace", "Palazzo Reale", 40.4172, -3.7143,
             note_en="With 3,418 rooms, it is the largest royal palace in Western Europe, used by the king for state ceremonies.",
             note_it="Con 3.418 stanze, è il palazzo reale più grande dell'Europa occidentale, usato dal re per le cerimonie di Stato."),
        # Source: Wikipedia, Royal Palace of Madrid; Wikipedia (es), Palacio Real de Madrid.
        Stop("madrid_almudena", "Almudena Cathedral", "Cattedrale dell'Almudena", 40.4155, -3.7141,
             note_en="Begun in 1883, the cathedral was consecrated by Pope John Paul II in 1993.",
             note_it="Iniziata nel 1883, la cattedrale fu consacrata da papa Giovanni Paolo II nel 1993."),
        # Source: Wikipedia, Almudena Cathedral.
        Stop("madrid_plaza_mayor", "Plaza Mayor", "Plaza Mayor", 40.4154, -3.7074,
             note_en="The square was finished in 1619, under Philip III, whose statue on horseback stands in the middle.",
             note_it="La piazza fu finita nel 1619, sotto Filippo III, la cui statua a cavallo sta al centro."),
        # Source: Wikipedia, Plaza Mayor, Madrid; Wikipedia (es), Plaza Mayor de Madrid.
        Stop("madrid_puerta_del_sol", "Puerta del Sol", "Puerta del Sol", 40.4169, -3.7036,
             note_en="On New Year's Eve, Spain eats twelve grapes to the twelve strokes of the clock on this square.",
             note_it="La notte di Capodanno la Spagna mangia dodici acini d'uva ai dodici rintocchi dell'orologio di questa piazza."),
        # Source: Wikipedia, Puerta del Sol; Wikipedia (es), Puerta del Sol.
        Stop("madrid_metropolis", "Metrópolis Building", "Edificio Metrópolis", 40.4188, -3.6977,
             note_en="Finished in 1911 and crowned by a winged Victory, it stands where the Gran Vía begins.",
             note_it="Finito nel 1911 e coronato da una Vittoria alata, sta dove comincia la Gran Vía."),
        # Source: Wikipedia, Metrópolis Building; Wikipedia (es), Edificio Metrópolis.
        Stop("madrid_cibeles", "Cibeles Fountain", "Fontana di Cibele", 40.4193, -3.6930,
             note_en="Real Madrid's fans celebrate their club's titles at this fountain of the goddess Cybele, finished in 1782.",
             note_it="I tifosi del Real Madrid festeggiano i titoli a questa fontana della dea Cibele, finita nel 1782."),
        # Source: Wikipedia, Plaza de Cibeles; Wikipedia (es), Fuente de Cibeles.
        Stop("madrid_prado", "Prado Museum", "Museo del Prado", 40.4138, -3.6925,
             note_en="Opened in 1819, the Prado keeps Velázquez's Las Meninas and Goya's Black Paintings.",
             note_it="Aperto nel 1819, il Prado custodisce Las Meninas di Velázquez e le pitture nere di Goya."),
        # Source: Wikipedia, Museo del Prado; Wikipedia (es), Museo del Prado.
        Stop("madrid_reina_sofia", "Reina Sofía Museum", "Museo Reina Sofía", 40.4080, -3.6944,
             note_en="Picasso's Guernica, painted in 1937 after the bombing of the Basque town, has hung here since 1992.",
             note_it="Il Guernica di Picasso, dipinto nel 1937 dopo il bombardamento della città basca, è esposto qui dal 1992."),
        # Source: Wikipedia, Guernica (Picasso); Wikipedia (es), Museo Nacional Centro de Arte Reina Sofía.
        Stop("madrid_angel_caido", "Fountain of the Fallen Angel", "Fontana dell'Angelo Caduto", 40.4084, -3.6826,
             note_en="Ricardo Bellver's statue shows Lucifer at the moment of his fall from heaven.",
             note_it="La statua di Ricardo Bellver mostra Lucifero nel momento della sua caduta dal cielo."),
        # Source: Wikipedia, Fuente del Ángel Caído; Wikipedia (es), Fuente del Ángel Caído.
        Stop("madrid_palacio_de_cristal", "Crystal Palace", "Palazzo di Cristallo", 40.4136, -3.6822,
             note_en="All iron and glass, it was built for the Philippines Exposition of 1887.",
             note_it="Tutto ferro e vetro, fu costruito per l'Esposizione delle Filippine del 1887."),
        # Source: Wikipedia, Palacio de Cristal.
        Stop("madrid_retiro_pond", "Retiro Pond", "Laghetto del Retiro", 40.4170, -3.6848,
             note_en="The Retiro was the kings' park until 1868; its great pond faces the monument to Alfonso XII.",
             note_it="Il Retiro fu il parco dei re fino al 1868; il suo grande laghetto guarda il monumento ad Alfonso XII."),
        # Source: Wikipedia, Buen Retiro Park.
        Stop("madrid_puerta_de_alcala", "Puerta de Alcalá", "Puerta de Alcalá", 40.4200, -3.6887,
             note_en="Charles III had Sabatini build this gate; finished in 1778, it is older than the Arc de Triomphe in Paris.",
             note_it="Carlo III la fece costruire dal Sabatini; finita nel 1778, è più antica dell'Arco di Trionfo di Parigi."),
        # Source: Wikipedia, Puerta de Alcalá; Wikipedia (es), Puerta de Alcalá.
    ],
)

BERLIN = Walk(
    id="BERLIN_WALL_VICTORY",
    city="berlin",
    city_en="Berlin",
    city_it="Berlino",
    route_en="From the Wall to the Victory Column, by the Brandenburg Gate",
    route_it="Dal Muro alla Colonna della Vittoria, passando per la Porta di Brandeburgo",
    outing_en="A walk in Berlin",
    outing_it="Passeggiata a Berlino",
    country="DE",
    continent="EUROPE",
    # The Spree (mapped in pieces: through the centre, past the Tiergarten, east of the island),
    # the Kupfergraben by Museum Island, the Humboldthafen and the Tiergarten's Neuer See; the
    # Landwehrkanal, narrow, as a line.
    water=[
        "relation/6306415", "way/4778262", "relation/6529251", "relation/7388656", "way/52189421",
        "relation/26993",
    ],
    canals=["relation/412199"],
    parks=["relation/7643526", "way/340138573", "way/23852021", "way/16000014"],
    stops=[
        Stop("berlin_wall_memorial", "Berlin Wall Memorial", "Memoriale del Muro di Berlino", 52.5351, 13.3903,
             note_en="Here the houses stood in the East and the pavement in the West: in 1961 people jumped from their windows to flee.",
             note_it="Qui le case erano a Est e il marciapiede a Ovest: nel 1961 c'era chi saltava dalle finestre per fuggire."),
        # Source: Wikipedia, Bernauer Straße; Wikipedia (de), Bernauer Straße.
        Stop("berlin_new_synagogue", "New Synagogue", "Nuova Sinagoga", 52.52482, 13.39445,
             note_en="Inaugurated in 1866 with about 3,000 seats, it was the largest synagogue in Berlin.",
             note_it="Inaugurata nel 1866 con circa 3.000 posti, era la sinagoga più grande di Berlino."),
        # Source: Wikipedia, New Synagogue (Berlin); Wikipedia (de), Neue Synagoge (Berlin).
        Stop("berlin_hackesche_hoefe", "Hackesche Höfe", "Hackesche Höfe", 52.5244, 13.4022,
             note_en="Eight courtyards open one into the next behind a single gateway; they opened in 1906.",
             note_it="Otto cortili si aprono uno nell'altro dietro un unico portone; furono inaugurati nel 1906."),
        # Source: Wikipedia, Hackesche Höfe; Wikipedia (de), Hackesche Höfe.
        Stop("berlin_tv_tower", "TV Tower", "Torre della televisione", 52.5208, 13.4094,
             note_en="Built by East Germany between 1965 and 1969, at 368 metres it is the tallest structure in Germany.",
             note_it="Costruita dalla Germania Est tra il 1965 e il 1969, con 368 metri è la struttura più alta della Germania."),
        # Source: Wikipedia, Fernsehturm Berlin; Wikipedia (de), Berliner Fernsehturm.
        Stop("berlin_cathedral", "Berlin Cathedral", "Duomo di Berlino", 52.5190, 13.4006,
             note_en="Finished in 1905 for Emperor William II, it holds the tombs of the Hohenzollern, Prussia's ruling house.",
             note_it="Finito nel 1905 per l'imperatore Guglielmo II, custodisce le tombe degli Hohenzollern, la casa regnante di Prussia."),
        # Source: Wikipedia, Berlin Cathedral; Wikipedia (de), Berliner Dom.
        Stop("berlin_bebelplatz", "Bebelplatz", "Bebelplatz", 52.5165, 13.3938,
             note_en="On 10 May 1933 books were burned here; a glass pane in the square looks down on empty shelves.",
             note_it="Il 10 maggio 1933 qui furono bruciati i libri; una lastra di vetro nella piazza mostra scaffali vuoti."),
        # Source: Wikipedia, Bebelplatz; Wikipedia (de), Bebelplatz.
        Stop("berlin_gendarmenmarkt", "Gendarmenmarkt", "Gendarmenmarkt", 52.5136, 13.3923,
             note_en="Two domed churches, the French and the German, flank the concert hall Schinkel built in 1821.",
             note_it="Due chiese con la cupola, la francese e la tedesca, affiancano la sala da concerto costruita da Schinkel nel 1821."),
        # Source: Wikipedia, Gendarmenmarkt; Wikipedia (de), Gendarmenmarkt.
        Stop("berlin_checkpoint_charlie", "Checkpoint Charlie", "Checkpoint Charlie", 52.5075, 13.3904,
             note_en="In October 1961, Soviet and American tanks faced each other at this crossing, ready to fire.",
             note_it="Nell'ottobre 1961 carri armati sovietici e americani si fronteggiarono a questo valico, pronti a sparare."),
        # Source: Wikipedia, Checkpoint Charlie; Wikipedia (de), Checkpoint Charlie.
        Stop("berlin_potsdamer_platz", "Potsdamer Platz", "Potsdamer Platz", 52.5096, 13.3760,
             note_en="Once one of the busiest squares in Europe, it lay empty along the Wall until 1989.",
             note_it="Un tempo tra le piazze più trafficate d'Europa, rimase vuota lungo il Muro fino al 1989."),
        # Source: Wikipedia, Potsdamer Platz; Wikipedia (de), Potsdamer Platz.
        Stop("berlin_holocaust_memorial", "Memorial to the Murdered Jews of Europe", "Memoriale agli ebrei assassinati d'Europa",
             52.5139, 13.3787,
             note_en="Peter Eisenman's field of 2,711 concrete slabs was opened in 2005.",
             note_it="Il campo di 2.711 blocchi di cemento di Peter Eisenman fu inaugurato nel 2005."),
        # Source: Wikipedia, Memorial to the Murdered Jews of Europe; Wikipedia (de), Denkmal für die ermordeten Juden Europas.
        Stop("berlin_brandenburg_gate", "Brandenburg Gate", "Porta di Brandeburgo", 52.5163, 13.3777,
             note_en="Napoleon carried the Quadriga on top of the gate off to Paris in 1806; it came back in 1814.",
             note_it="Napoleone portò a Parigi la Quadriga in cima alla porta nel 1806; tornò nel 1814."),
        # Source: Wikipedia, Brandenburg Gate; Wikipedia (de), Brandenburger Tor.
        Stop("berlin_reichstag", "Reichstag", "Reichstag", 52.5186, 13.3748,
             note_en="The Bundestag has sat here since 1999, under a glass dome open to visitors.",
             note_it="Il Bundestag siede qui dal 1999, sotto una cupola di vetro aperta ai visitatori."),
        # Source: Wikipedia, Reichstag building; Wikipedia (de), Reichstagsgebäude.
        Stop("berlin_victory_column", "Victory Column", "Colonna della Vittoria", 52.5145, 13.3501,
             note_en="It first stood before the Reichstag; in 1939 it was moved here, to the Großer Stern.",
             note_it="Sorgeva davanti al Reichstag; nel 1939 fu spostata qui, al Großer Stern."),
        # Source: Wikipedia, Victory Column (Berlin); Wikipedia (de), Siegessäule (Berlin).
    ],
)

VIENNA = Walk(
    id="VIENNA_BELVEDERE_PRATER",
    city="vienna",
    city_en="Vienna",
    city_it="Vienna",
    route_en="From the Belvedere to the Prater, by the Ring and St Stephen's",
    route_it="Dal Belvedere al Prater, passando per il Ring e Santo Stefano",
    outing_en="A walk in Vienna",
    outing_it="Passeggiata a Vienna",
    country="AT",
    continent="EUROPE",
    # The Danube Canal, the Wien river through the Stadtpark, and the Stadtpark's pond.
    water=["relation/65901", "relation/21407746", "relation/2577803"],
    parks=[
        "relation/7000697", "way/12988858", "way/28151223", "way/8063831", "way/8063768", "relation/7735480",
        "way/551031461", "way/8044066",
    ],
    stops=[
        Stop("vienna_belvedere", "Upper Belvedere", "Belvedere Superiore", 48.1915, 16.3809,
             note_en="Built as the summer palace of Prince Eugene of Savoy, it now keeps Klimt's The Kiss.",
             note_it="Costruito come residenza estiva del principe Eugenio di Savoia, oggi custodisce Il bacio di Klimt."),
        # Source: Wikipedia, Belvedere, Vienna; Wikipedia (de), Schloss Belvedere and Österreichische Galerie Belvedere.
        Stop("vienna_karlskirche", "Karlskirche", "Karlskirche", 48.1980, 16.3719,
             note_en="In 1713 Emperor Charles VI vowed this church to Saint Charles Borromeo, a protector against the plague.",
             note_it="Nel 1713 l'imperatore Carlo VI fece voto di questa chiesa a san Carlo Borromeo, protettore contro la peste."),
        # Source: Wikipedia, Karlskirche; Wikipedia (de), Karlskirche (Wien). The two disagree on
        # whether the vow came during the plague or a year after it: neither is said.
        Stop("vienna_secession", "Secession Building", "Palazzo della Secessione", 48.2005, 16.3660,
             note_en="Finished in 1898, it carries its motto in gold: To every age its art, to art its freedom.",
             note_it="Finito nel 1898, porta in oro il suo motto: A ogni epoca la sua arte, all'arte la sua libertà."),
        # Source: Wikipedia, Secession Building; Wikipedia (de), Wiener Secessionsgebäude.
        Stop("vienna_state_opera", "State Opera", "Opera di Stato", 48.2030, 16.3692,
             note_en="The opera house opened in 1869 with Mozart's Don Giovanni.",
             note_it="Il teatro dell'opera fu inaugurato nel 1869 con il Don Giovanni di Mozart."),
        # Source: Wikipedia, Vienna State Opera; Wikipedia (de), Wiener Staatsoper.
        Stop("vienna_maria_theresien_platz", "Maria-Theresien-Platz", "Maria-Theresien-Platz", 48.2045, 16.3609,
             note_en="Two twin museums, of art history and of natural history, face each other across Maria Theresa's monument.",
             note_it="Due musei gemelli, di storia dell'arte e di storia naturale, si guardano attraverso il monumento a Maria Teresa."),
        # Source: Wikipedia, Maria-Theresien-Platz; Wikipedia (de), Maria-Theresien-Platz.
        Stop("vienna_parliament", "Parliament", "Parlamento", 48.2081, 16.3592,
             note_en="Theophil Hansen built it in the Greek style, with Pallas Athena standing before it.",
             note_it="Theophil Hansen lo costruì in stile greco, con Pallade Atena in piedi davanti."),
        # Source: Wikipedia, Austrian Parliament Building; Wikipedia (de), Parlamentsgebäude (Wien).
        Stop("vienna_city_hall", "City Hall", "Municipio", 48.2106, 16.3576,
             note_en="Finished in 1883, the neo-Gothic city hall has the Rathausmann, a standard-bearer, on top of its tower.",
             note_it="Finito nel 1883, il municipio neogotico ha in cima alla torre il Rathausmann, un portabandiera."),
        # Source: Wikipedia, Vienna City Hall; Wikipedia (de), Wiener Rathaus.
        Stop("vienna_hofburg", "Hofburg", "Hofburg", 48.2080, 16.3664,
             note_en="The Habsburgs ruled from this palace, their winter residence, until 1918.",
             note_it="Gli Asburgo governarono da questo palazzo, la loro residenza invernale, fino al 1918."),
        # Source: Wikipedia, Hofburg; Wikipedia (de), Hofburg.
        Stop("vienna_plague_column", "Plague Column", "Colonna della peste", 48.2087, 16.3698,
             note_en="Emperor Leopold I vowed it during the plague of 1679, in the middle of the Graben.",
             note_it="L'imperatore Leopoldo I ne fece voto durante la peste del 1679, in mezzo al Graben."),
        # Source: Wikipedia, Vienna Plague Column; Wikipedia (de), Wiener Pestsäule.
        Stop("vienna_stephansdom", "St Stephen's Cathedral", "Duomo di Santo Stefano", 48.2085, 16.3724,
             note_en="Its south tower rises 136 metres; its great bell, the Pummerin, was first cast from Ottoman cannons.",
             note_it="La sua torre sud si alza per 136 metri; la sua grande campana, la Pummerin, fu fusa la prima volta con cannoni ottomani."),
        # Source: Wikipedia, St. Stephen's Cathedral, Vienna; Wikipedia (de), Stephansdom.
        Stop("vienna_stadtpark", "Stadtpark", "Stadtpark", 48.2029, 16.3797,
             note_en="Opened in 1862, the park keeps the gilded statue of Johann Strauss II.",
             note_it="Aperto nel 1862, il parco custodisce la statua dorata di Johann Strauss figlio."),
        # Source: Wikipedia, Stadtpark, Vienna; Wikipedia (de), Wiener Stadtpark.
        Stop("vienna_hundertwasserhaus", "Hundertwasserhaus", "Hundertwasserhaus", 48.2074, 16.3939,
             note_en="Council flats finished in 1985, with undulating floors and trees growing from the rooms.",
             note_it="Case popolari finite nel 1985, con pavimenti ondulati e alberi che crescono dalle stanze."),
        # Source: Wikipedia, Hundertwasserhaus; Wikipedia (de), Hundertwasserhaus (Wien).
        Stop("vienna_riesenrad", "Giant Ferris Wheel", "Ruota panoramica", 48.2167, 16.3959,
             note_en="Built in 1897, the Prater's wheel turns in a famous scene of the film The Third Man.",
             note_it="Costruita nel 1897, la ruota del Prater gira in una famosa scena del film Il terzo uomo."),
        # Source: Wikipedia, Wiener Riesenrad; Wikipedia (de), Wiener Riesenrad.
    ],
)

PORTO = Walk(
    id="PORTO_SE_PILAR",
    city="porto",
    city_en="Porto",
    city_it="Porto",
    route_en="From the cathedral to the Serra do Pilar, across the Douro",
    route_it="Dalla cattedrale alla Serra do Pilar, oltre il Douro",
    outing_en="A walk in Porto",
    outing_it="Passeggiata a Porto",
    country="IBERIA",
    continent="EUROPE",
    water=["relation/3688750"],
    parks=["way/244599647", "way/215304932", "way/98836111", "relation/3251867"],
    stops=[
        # A short walk (about 5 km, ADR 0015 decision 10). It begins where the Camino Portugués
        # begins, at the cathedral, and ends on the Gaia bank, looking back at the old town.
        Stop("porto_se", "Porto Cathedral", "Cattedrale di Porto", 41.1428, -8.6112,
             note_en="Begun in the 12th century, the cathedral is where the Camino Portugués, one of the Ways, sets out for Santiago.",
             note_it="Iniziata nel XII secolo, la cattedrale è il punto di partenza del Cammino Portoghese, uno dei Cammini, verso Santiago."),
        # Source: Wikipedia, Porto Cathedral (the second half of the 12th century);
        # Wikipedia (pt), Sé do Porto (the first half): the century alone. The Way: this app.
        Stop("porto_sao_bento", "São Bento Station", "Stazione di São Bento", 41.1455, -8.6105,
             note_en="In its hall, Jorge Colaço's azulejos, put up from 1905, show scenes from the history of Portugal.",
             note_it="Nell'atrio, gli azulejos di Jorge Colaço, posati dal 1905, raccontano scene della storia del Portogallo."),
        # Source: Wikipedia, São Bento railway station; Wikipedia (pt), Estação Ferroviária de
        # Porto-São Bento. They disagree on the count of tiles and on the last year: neither said.
        Stop("porto_bolhao", "Bolhão Market", "Mercato del Bolhão", 41.1493, -8.6070,
             note_en="Built in 1914 in reinforced concrete and iron, the market reopened in 2022 after four years of restoration.",
             note_it="Costruito nel 1914 in cemento armato e ferro, il mercato ha riaperto nel 2022 dopo quattro anni di restauro."),
        # Source: Wikipedia (pt), Mercado do Bolhão; European Commission, Inforegio (13 Jan 2026).
        Stop("porto_aliados", "Avenida dos Aliados", "Avenida dos Aliados", 41.1473, -8.6111,
             note_en="Laid out from 1916 in place of a whole neighbourhood, the avenue is named after the Allies of the First World War.",
             note_it="Tracciato dal 1916 al posto di un intero quartiere, il viale porta il nome degli Alleati della Prima guerra mondiale."),
        # Source: Wikipedia (pt), Avenida dos Aliados; Porto's tourist guides (the Laranjal
        # neighbourhood pulled down for it).
        Stop("porto_clerigos", "Clérigos Tower", "Torre dos Clérigos", 41.1457, -8.6146,
             note_en="Nicolau Nasoni's bell tower, finished in 1763, rises 75 metres over the city.",
             note_it="Il campanile di Nicolau Nasoni, finito nel 1763, si alza per 75 metri sulla città."),
        # Source: Wikipedia, Clérigos Church; Wikipedia (pt), Igreja e Torre dos Clérigos. They
        # disagree on the steps (240, 225): not said.
        Stop("porto_bolsa", "Palácio da Bolsa", "Palácio da Bolsa", 41.1413, -8.6160,
             note_en="Porto's merchants began their exchange in 1842; its Arab Room, in the Moorish style, was built from 1862 to 1880.",
             note_it="I mercanti di Porto iniziarono la loro Borsa nel 1842; il suo Salone Arabo, in stile moresco, fu realizzato tra il 1862 e il 1880."),
        # Source: Wikipedia, Palácio da Bolsa; Wikipedia (pt), Palácio da Bolsa.
        Stop("porto_ribeira", "Ribeira", "Ribeira", 41.1407, -8.6130),
        # No sentence: no second source was found to check one against.
        Stop("porto_ponte_luis", "Dom Luís I Bridge", "Ponte Dom Luís I", 41.1399, -8.6094,
             note_en="Built from 1881 to 1886 by Théophile Seyrig, once Gustave Eiffel's partner, its iron arch spans 172 metres.",
             note_it="Costruito dal 1881 al 1886 da Théophile Seyrig, già socio di Gustave Eiffel, il suo arco di ferro misura 172 metri."),
        # Source: Wikipedia, Dom Luís I Bridge; Wikipedia (pt), Ponte de D. Luís (Porto). The
        # walk crosses by the lower deck, at the river.
        Stop("porto_gaia", "The port wine cellars", "Le cantine del Porto", 41.1376, -8.6125,
             note_en="Port wine ages in the cellars of Vila Nova de Gaia, where rabelo boats once brought it down the Douro.",
             note_it="Il vino Porto invecchia nelle cantine di Vila Nova de Gaia, dove un tempo lo portavano giù per il Douro le barche rabelo."),
        # Source: Wikipedia, Port wine; Wikipedia (pt), Vinho do Porto.
        Stop("porto_serra_do_pilar", "Serra do Pilar Monastery", "Monastero della Serra do Pilar", 41.1384, -8.6081,
             note_en="Its church and its cloister are both round, and of the same diameter: a convent unique of its kind.",
             note_it="La chiesa e il chiostro sono entrambi circolari, dello stesso diametro: un convento unico nel suo genere."),
        # Source: Wikipedia, Monastery of Serra do Pilar; Wikipedia (pt), Mosteiro da Serra do
        # Pilar.
    ],
)

AMSTERDAM = Walk(
    id="AMSTERDAM_CENTRAAL_WESTERKERK",
    city="amsterdam",
    city_en="Amsterdam",
    city_it="Amsterdam",
    route_en="From Centraal Station to the Westerkerk, along the canals",
    route_it="Dalla Stazione Centrale alla Westerkerk, lungo i canali",
    outing_en="A walk in Amsterdam",
    outing_it="Passeggiata ad Amsterdam",
    country="NL",
    continent="EUROPE",
    # The IJ and the docks are large areas listed here; the canals are read from the tiles.
    water=["relation/554702", "relation/8878552", "relation/8730108", "relation/12113080", "relation/14235038"],
    water_from_tiles=True,
    parks=["way/25965389", "relation/17080648", "way/31527079", "relation/20165014", "way/26446429"],
    stops=[
        # A short walk (about 5 km, ADR 0015 decision 10).
        Stop("amsterdam_centraal", "Centraal Station", "Stazione Centrale", 52.3789, 4.9006,
             note_en="Pierre Cuypers, who also designed the Rijksmuseum, built the station, opened in 1889, on three artificial islands and 8,687 wooden piles.",
             note_it="Pierre Cuypers, che progettò anche il Rijksmuseum, costruì la stazione, aperta nel 1889, su tre isole artificiali e 8.687 pali di legno."),
        # Source: Wikipedia, Amsterdam Centraal station; Wikipedia (nl), Station Amsterdam Centraal.
        Stop("amsterdam_oude_kerk", "Oude Kerk", "Oude Kerk", 52.3744, 4.8981,
             note_en="Amsterdam's oldest building, consecrated in 1306; Rembrandt's wife, Saskia, was buried here in 1642.",
             note_it="L'edificio più antico di Amsterdam, consacrato nel 1306; qui fu sepolta nel 1642 Saskia, la moglie di Rembrandt."),
        # Source: Wikipedia, Oude Kerk (Amsterdam); Wikipedia (nl), Oude Kerk (Amsterdam).
        Stop("amsterdam_waag", "De Waag", "De Waag", 52.3727, 4.9004,
             note_en="A city gate turned weigh house in 1617; the surgeons' guild upstairs commissioned Rembrandt's Anatomy Lesson of Dr Tulp.",
             note_it="Una porta della città diventata pesa pubblica nel 1617; la corporazione dei chirurghi, al piano di sopra, commissionò a Rembrandt la Lezione di anatomia del dottor Tulp."),
        # Source: Wikipedia, Waag (Amsterdam); Wikipedia (nl), Waag (Amsterdam).
        Stop("amsterdam_rembrandthuis", "Rembrandt House", "Casa di Rembrandt", 52.3694, 4.9012,
             note_en="Rembrandt bought the house in 1639 and lived here until 1658, when it was auctioned after his bankruptcy.",
             note_it="Rembrandt comprò la casa nel 1639 e vi abitò fino al 1658, quando fu messa all'asta dopo il suo fallimento."),
        # Source: Wikipedia, Rembrandt House Museum; Wikipedia (nl), Rembrandthuis.
        Stop("amsterdam_magere_brug", "Magere Brug", "Magere Brug", 52.3636, 4.9024,
             note_en="The Skinny Bridge was first built in 1691, of wood and narrower than the stone bridge planned; it was opened by hand until 1994.",
             note_it="Il Ponte Magro fu costruito la prima volta nel 1691, di legno e più stretto del ponte di pietra previsto; fu aperto a mano fino al 1994."),
        # Source: Wikipedia, Magere Brug; Wikipedia (nl), Magere Brug. They disagree on the
        # lights (1,200, 1,800): not said.
        Stop("amsterdam_bloemenmarkt", "The flower market", "Il mercato dei fiori", 52.3669, 4.8908,
             note_en="The flower market has stood on the Singel since 1862, where flowers were once sold from boats on the canal.",
             note_it="Il mercato dei fiori è sul Singel dal 1862; un tempo i fiori si vendevano dalle barche ormeggiate nel canale."),
        # Source: Wikipedia, Bloemenmarkt; Wikipedia (nl), Bloemenmarkt.
        Stop("amsterdam_begijnhof", "Begijnhof", "Begijnhof", 52.3694, 4.8900,
             note_en="First named as a courtyard in 1389, it was home to beguines until the last of them died in 1971.",
             note_it="Citato come cortile per la prima volta nel 1389, ospitò le beghine fino alla morte dell'ultima, nel 1971."),
        # Source: Wikipedia, Begijnhof, Amsterdam; Wikipedia (nl), Begijnhof (Amsterdam). They
        # disagree on the wooden house's age (about 1420, about 1528): not said.
        Stop("amsterdam_dam", "The Royal Palace", "Il Palazzo Reale", 52.3731, 4.8931,
             note_en="Built as the city hall on 13,659 wooden piles, it became a royal palace for Louis Bonaparte in 1808.",
             note_it="Costruito come municipio su 13.659 pali di legno, divenne nel 1808 il palazzo reale di Luigi Bonaparte."),
        # Source: Wikipedia, Royal Palace of Amsterdam; Wikipedia (nl), Koninklijk Paleis
        # Amsterdam. On the Dam.
        Stop("amsterdam_westerkerk", "Westerkerk", "Westerkerk", 52.3746, 4.8840,
             note_en="Its 87-metre tower is the tallest church tower in Amsterdam; Rembrandt was buried here in 1669, in a grave now lost.",
             note_it="La sua torre di 87 metri è il campanile più alto di Amsterdam; qui fu sepolto Rembrandt nel 1669, in una tomba oggi perduta."),
        # Source: Wikipedia, Westerkerk; Wikipedia (nl), Westerkerk (Amsterdam).
    ],
)

PRAGUE = Walk(
    id="PRAGUE_CASTLE_WENCESLAS",
    city="prague",
    city_en="Prague",
    city_it="Praga",
    route_en="From the Castle to Wenceslas Square, over Charles Bridge",
    route_it="Dal Castello a Piazza San Venceslao, passando per Ponte Carlo",
    outing_en="A walk in Prague",
    outing_it="Passeggiata a Praga",
    country="CZ",
    continent="EUROPE",
    water=["relation/19221"],
    # The Old Town's lanes are mapped as residential streets, as Milan's and Rome's centres
    # are: without the longer ones, its map was the emptiest of all.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    parks=[
        "way/903579699", "relation/12271961", "relation/10507680", "relation/14239612", "relation/14239610",
        "relation/14239611", "relation/14239613", "way/27581161", "relation/7078342", "way/26328480",
        "relation/13480459", "way/24984378",
    ],
    stops=[
        # A short walk (about 5 km, ADR 0015 decision 10), downhill from the Castle.
        Stop("prague_castle", "Prague Castle", "Castello di Praga", 50.0896, 14.3977,
             note_en="By the Guinness records the largest ancient castle in the world, it has been the president's seat since 1918.",
             note_it="Secondo il Guinness il più grande castello antico del mondo, è la sede del presidente dal 1918."),
        # Source: Wikipedia, Prague Castle; Wikipedia (cs), Pražský hrad.
        Stop("prague_st_vitus", "St Vitus Cathedral", "Cattedrale di San Vito", 50.0909, 14.4006,
             note_en="Begun in 1344 and finished only in 1929, the cathedral keeps the Bohemian crown jewels behind a door with seven locks.",
             note_it="Iniziata nel 1344 e finita solo nel 1929, la cattedrale custodisce i gioielli della corona boema dietro una porta con sette serrature."),
        # Source: Wikipedia, St. Vitus Cathedral; Wikipedia (cs), Katedrála svatého Víta,
        # Václava a Vojtěcha.
        Stop("prague_st_nicholas", "St Nicholas Church", "Chiesa di San Nicola", 50.0880, 14.4032,
             note_en="Mozart played its organ, of over 4,000 pipes, in 1787; under communism its tower was a secret police post watching the embassies.",
             note_it="Mozart suonò il suo organo, di oltre 4.000 canne, nel 1787; durante il comunismo il campanile fu un posto di osservazione della polizia segreta sulle ambasciate."),
        # Source: Wikipedia, St. Nicholas Church (Malá Strana); Wikipedia (cs), Kostel svatého
        # Mikuláše (Malá Strana).
        Stop("prague_charles_bridge", "Charles Bridge", "Ponte Carlo", 50.0865, 14.4114,
             note_en="Charles IV laid its first stone in 1357; 30 statues, most of them Baroque, line its 516 metres.",
             note_it="Carlo IV ne posò la prima pietra nel 1357; 30 statue, quasi tutte barocche, ne accompagnano i 516 metri."),
        # Source: Wikipedia, Charles Bridge; Wikipedia (cs), Karlův most.
        Stop("prague_klementinum", "Klementinum", "Klementinum", 50.0867, 14.4161,
             note_en="A Jesuit college from 1556, where the weather has been recorded since 1775, without a break to this day.",
             note_it="Collegio dei gesuiti dal 1556, qui il tempo si registra dal 1775, senza interruzioni fino a oggi."),
        # Source: Wikipedia, Clementinum; Wikipedia (cs), Klementinum.
        Stop("prague_orloj", "The Astronomical Clock", "L'orologio astronomico", 50.0870, 14.4207,
             note_en="Made in 1410, it is the oldest astronomical clock still working; each hour of the day the twelve apostles appear above its dial.",
             note_it="Costruito nel 1410, è il più antico orologio astronomico ancora in funzione; ogni ora del giorno i dodici apostoli compaiono sopra il quadrante."),
        # Source: Wikipedia, Prague astronomical clock; Wikipedia (cs), Staroměstský orloj.
        Stop("prague_old_new_synagogue", "Old-New Synagogue", "Sinagoga Vecchia-Nuova", 50.0900, 14.4186,
             note_en="Built in the 13th century, it is among Europe's oldest synagogues still in use; legend puts the Golem in its attic.",
             note_it="Costruita nel XIII secolo, è tra le più antiche sinagoghe d'Europa ancora in uso; la leggenda vuole il Golem nella sua soffitta."),
        # Source: Wikipedia, Old New Synagogue (1270); Wikipedia (cs), Staronová synagoga (the
        # second half of the 13th century): the century alone.
        Stop("prague_powder_tower", "Powder Tower", "Torre delle Polveri", 50.0872, 14.4278,
             note_en="Begun in 1475 as a gate to the Old Town, the 65-metre tower is where the Royal Route to the Castle begins.",
             note_it="Iniziata nel 1475 come porta della Città Vecchia, la torre di 65 metri segna l'inizio della Via Reale verso il Castello."),
        # Source: Wikipedia, Powder Tower, Prague; Wikipedia (cs), Prašná brána. They disagree
        # on whether it ever held gunpowder: not said.
        Stop("prague_wenceslas", "Wenceslas Square", "Piazza San Venceslao", 50.0798, 14.4297,
             note_en="Founded as the Horse Market in 1348, the square filled with the crowds of the Velvet Revolution in November 1989.",
             note_it="Nata come Mercato dei Cavalli nel 1348, la piazza si riempì delle folle della Rivoluzione di velluto nel novembre 1989."),
        # Source: Wikipedia, Wenceslas Square; Wikipedia (cs), Václavské náměstí. They disagree
        # on the statue's year (1912, 1913): not said.
    ],
)

LIMA = Walk(
    id="LIMA_SAN_MARTIN_RESERVA",
    city="lima",
    city_en="Lima",
    city_it="Lima",
    route_en="The historic centre, from Plaza San Martín to the Parque de la Reserva",
    route_it="Il centro storico, da Plaza San Martín al Parque de la Reserva",
    outing_en="A walk in Lima",
    outing_it="Passeggiata a Lima",
    country="PE",
    continent="AMERICAS",
    water=["way/367932671", "way/402037791", "way/402037790", "way/402037789"],
    parks=["way/39413088", "relation/12175742", "way/44364384", "way/117755695", "way/172243636"],
    stops=[
        Stop("lima_plaza_san_martin", "Plaza San Martín", "Plaza San Martín", -12.0517, -77.0346,
             note_en="Opened on 27 July 1921 for the centenary of independence, it honours the liberator José de San Martín.",
             note_it="Inaugurata il 27 luglio 1921 per il centenario dell'indipendenza, onora il liberatore José de San Martín."),
        # Source: Wikipedia, Plaza San Martín (Lima); Wikipedia (es), Plaza San Martín (Lima).
        Stop("lima_jiron_union", "Jirón de la Unión", "Jirón de la Unión", -12.0490, -77.0332,
             note_en="The street joins Lima's two great squares, in a historic centre that is a UNESCO World Heritage Site.",
             note_it="La via unisce le due grandi piazze di Lima, in un centro storico patrimonio dell'umanità UNESCO."),
        # Source: Wikipedia (es), Jirón de la Unión; Wikipedia, Historic Centre of Lima.
        Stop("lima_plaza_de_armas", "Plaza de Armas", "Plaza de Armas", -12.0461, -77.0303,
             note_en="Francisco Pizarro founded Lima on this square on 18 January 1535, as the City of the Kings.",
             note_it="Francisco Pizarro fondò Lima su questa piazza il 18 gennaio 1535, come Città dei Re."),
        # Source: Wikipedia, Plaza Mayor, Lima; Wikipedia (es), Plaza Mayor de Lima.
        Stop("lima_cathedral", "Cathedral of Lima", "Cattedrale di Lima", -12.0466, -77.0296,
             note_en="Pizarro laid its first stone in 1535; the cathedral took its present form between 1602 and 1797.",
             note_it="Pizarro ne posò la prima pietra nel 1535; la cattedrale prese la forma di oggi tra il 1602 e il 1797."),
        # Source: Wikipedia, Cathedral of Lima; Wikipedia (es), Catedral de Lima.
        Stop("lima_government_palace", "Government Palace", "Palazzo del Governo", -12.0453, -77.0302,
             note_en="The president's palace stands on the plot Pizarro kept for his own house when he founded the city.",
             note_it="Il palazzo del presidente sorge sul terreno che Pizarro tenne per la sua casa quando fondò la città."),
        # Source: Wikipedia, Government Palace (Peru); Wikipedia (es), Palacio de Gobierno del Perú.
        Stop("lima_casa_aliaga", "Casa de Aliaga", "Casa de Aliaga", -12.0445, -77.0303,
             note_en="Built for Jerónimo de Aliaga in 1536, it has been home to the same family for seventeen generations.",
             note_it="Costruita per Jerónimo de Aliaga nel 1536, è la casa della stessa famiglia da diciassette generazioni."),
        # Source: Wikipedia, Casa de Aliaga; Wikipedia (es), Casa de Aliaga.
        Stop("lima_santo_domingo", "Santo Domingo", "Santo Domingo", -12.0439, -77.0318,
             note_en="The University of San Marcos, founded in 1551, grew out of the lessons first given in this convent.",
             note_it="L'Università di San Marcos, fondata nel 1551, nacque dalle lezioni tenute in questo convento."),
        # Source: Wikipedia, National University of San Marcos.
        Stop("lima_puente_de_piedra", "Puente de Piedra", "Puente de Piedra", -12.0429, -77.0297,
             note_en="Built in 1610, after a flood of the Rímac had carried away the bridge before it.",
             note_it="Costruito nel 1610, dopo che una piena del Rímac aveva portato via il ponte precedente."),
        # Source: Wikipedia (es), Puente de Piedra (Lima).
        Stop("lima_alameda_descalzos", "Alameda de los Descalzos", "Alameda de los Descalzos", -12.0357, -77.0258,
             note_en="Laid out in 1611, the avenue copies the Alameda de Hércules of Seville.",
             note_it="Tracciato nel 1611, il viale riprende l'Alameda de Hércules di Siviglia."),
        # Source: Wikipedia, Alameda de los Descalzos; Wikipedia (es), Alameda de los Descalzos.
        Stop("lima_muralla", "Parque de la Muralla", "Parque de la Muralla", -12.0446, -77.0263,
             note_en="The park keeps a stretch of the walls built against pirates in the 1680s, which never saw a battle.",
             note_it="Il parco conserva un tratto delle mura costruite contro i pirati negli anni Ottanta del Seicento, che non videro mai una battaglia."),
        # Source: Wikipedia, Walls of Lima; Wikipedia (es), Parque de La Muralla.
        Stop("lima_san_francisco", "San Francisco", "San Francisco", -12.0456, -77.0272,
             note_en="Its catacombs were the city's cemetery in colonial times; the convent is a UNESCO World Heritage Site.",
             note_it="Le sue catacombe erano il cimitero della città in epoca coloniale; il convento è patrimonio dell'umanità UNESCO."),
        # Source: Wikipedia, Basilica and Convent of San Francisco, Lima; Wikipedia, Historic Centre of Lima.
        Stop("lima_barrio_chino", "Chinatown", "Quartiere cinese", -12.0513, -77.0253,
             note_en="Lima's Chinatown made the chifas famous, as Peru calls its Chinese restaurants.",
             note_it="Il quartiere cinese di Lima ha reso famosi i chifa, come in Perù si chiamano i ristoranti cinesi."),
        # Source: Wikipedia, Barrio Chino (Lima); Wikipedia (es), Barrio chino de Lima.
        Stop("lima_palace_of_justice", "Palace of Justice", "Palazzo di Giustizia", -12.0576, -77.0350,
             note_en="Opened in 1939, it was modelled on the Palace of Justice in Brussels.",
             note_it="Inaugurato nel 1939, fu ispirato al Palazzo di Giustizia di Bruxelles."),
        # Source: Wikipedia, Palace of Justice, Lima; Wikipedia (es), Palacio de Justicia del Perú.
        Stop("lima_mali", "Lima Art Museum", "Museo d'Arte di Lima", -12.0608, -77.0368,
             note_en="The museum is in the Palacio de la Exposición, built for Lima's International Exhibition of 1872.",
             note_it="Il museo è nel Palacio de la Exposición, costruito per l'Esposizione internazionale di Lima del 1872."),
        # Source: Wikipedia, Museo de Arte de Lima; Wikipedia (es), Museo de Arte de Lima.
        Stop("lima_parque_de_la_reserva", "Parque de la Reserva", "Parque de la Reserva", -12.0705, -77.0337,
             note_en="Named for the reservists who defended Lima in the War of the Pacific, at San Juan and Miraflores.",
             note_it="Prende il nome dai riservisti che difesero Lima nella guerra del Pacifico, a San Juan e Miraflores."),
        # Source: Wikipedia (es), Parque de la Reserva.
    ],
)

CUSCO = Walk(
    id="CUSCO_ARMAS_QORIKANCHA",
    city="cusco",
    city_en="Cusco",
    city_it="Cusco",
    route_en="From the Plaza de Armas up to Sacsayhuamán, and down to the Qorikancha",
    route_it="Dalla Plaza de Armas su a Sacsayhuamán, e giù fino al Qorikancha",
    outing_en="A walk in Cusco",
    outing_it="Passeggiata a Cusco",
    country="PE",
    continent="AMERICAS",
    # No water: the rivers of the old centre run in channels, mostly covered, and the Huatanay
    # begins south of the map.
    parks=["way/83130621"],
    more_streets=("residential", "unclassified"),
    stops=[
        Stop("cusco_plaza_de_armas", "Plaza de Armas", "Plaza de Armas", -13.5168, -71.9789,
             note_en="This was the heart of the Inca capital; the City of Cusco has been a UNESCO World Heritage Site since 1983.",
             note_it="Qui batteva il cuore della capitale inca; la città di Cusco è patrimonio dell'umanità UNESCO dal 1983."),
        # Source: Wikipedia, Cusco; Wikipedia (es), Plaza Regocijo (the Inca square).
        Stop("cusco_compania", "La Compañía", "La Compañía", -13.5175, -71.9781,
             note_en="The Jesuits built their church on Amarucancha, the palace of the Inca Huayna Capac.",
             note_it="I gesuiti costruirono la loro chiesa sull'Amarucancha, il palazzo dell'inca Huayna Cápac."),
        # Source: Wikipedia (es), Iglesia de la Compañía de Jesús (Cusco).
        Stop("cusco_cathedral", "Cathedral of Cusco", "Cattedrale di Cusco", -13.5162, -71.9781,
             note_en="In its Last Supper, attributed to Marcos Zapata, a guinea pig is on the table.",
             note_it="Nella sua Ultima Cena, attribuita a Marcos Zapata, in tavola c'è un porcellino d'India."),
        # Source: Wikipedia, Marcos Zapata.
        Stop("cusco_twelve_angled_stone", "Twelve-angled stone", "Pietra dei dodici angoli", -13.5157, -71.9762,
             note_en="In Hatun Rumiyoc street, the stone of twelve angles is set in the wall of an Inca palace.",
             note_it="In calle Hatun Rumiyoc, la pietra dei dodici angoli è incastonata nel muro di un palazzo inca."),
        # Source: Wikipedia, Twelve-angled stone; Wikipedia (es), Piedra de los doce ángulos.
        Stop("cusco_san_blas", "San Blas", "San Blas", -13.5152, -71.9742,
             note_en="The church of San Blas keeps a pulpit carved in cedar, a marvel of Churrigueresque woodwork.",
             note_it="La chiesa di San Blas custodisce un pulpito intagliato nel cedro, una meraviglia di intaglio churrigueresco."),
        # Source: Wikipedia (es), Iglesia de San Blas (Cusco).
        Stop("cusco_sacsayhuaman", "Sacsayhuamán", "Sacsayhuamán", -13.5085, -71.9820,
             note_en="Its largest stone weighs over a hundred tonnes; the Inti Raymi, the Inca feast of the sun, is held nearby every 24 June.",
             note_it="La sua pietra più grande pesa oltre cento tonnellate; qui vicino si celebra ogni 24 giugno l'Inti Raymi, la festa inca del sole."),
        # Source: Wikipedia, Sacsayhuamán.
        Stop("cusco_cristo_blanco", "Cristo Blanco", "Cristo Blanco", -13.5098, -71.9779,
             note_en="The white Christ, eight metres tall, was a gift to the city from its Palestinian Christians, in 1945.",
             note_it="Il Cristo bianco, alto otto metri, fu un regalo alla città dei cristiani palestinesi, nel 1945."),
        # Source: Wikipedia (es), Cristo Blanco.
        Stop("cusco_qenqo", "Q'enqo", "Q'enqo", -13.5091, -71.9704,
             note_en="An Inca huaca, a sacred place carved into the rock, with a shrine hollowed out beneath it.",
             note_it="Una huaca inca, un luogo sacro scolpito nella roccia, con un santuario scavato al di sotto."),
        # Source: Wikipedia, Kenko.
        Stop("cusco_san_cristobal", "San Cristóbal", "San Cristóbal", -13.5134, -71.9802,
             note_en="Paullu Inca, brother of Atahualpa, founded this church; his remains were found beneath it in 2007.",
             note_it="Paullu Inca, fratello di Atahualpa, fondò questa chiesa; i suoi resti vi furono trovati sotto nel 2007."),
        # Source: Wikipedia (es), Iglesia de San Cristóbal (Cusco).
        Stop("cusco_plaza_regocijo", "Plaza Regocijo", "Plaza Regocijo", -13.5170, -71.9802,
             note_en="Its Quechua name, Kusipata, means the place of joy; it was part of the great Inca square.",
             note_it="Il suo nome quechua, Kusipata, significa luogo della gioia; faceva parte della grande piazza inca."),
        # Source: Wikipedia (es), Plaza Regocijo.
        Stop("cusco_san_pedro", "San Pedro Market", "Mercato di San Pedro", -13.5212, -71.9825,
             note_en="Cusco's central market since 1925, declared part of Peru's cultural heritage in 2024, it sells fruit, meat and cooked food.",
             note_it="Mercato centrale di Cusco dal 1925, dichiarato patrimonio culturale del Perù nel 2024, vende frutta, carne e piatti pronti."),
        # Source: Wikipedia (es), Mercado Central de San Pedro; Andina (Peru's state news agency),
        # "Cusco: Gore reconoce al Mercado Central San Pedro en su centenario de fundación", 2025.
        # Its first iron structure is often credited to Gustave Eiffel, who died in 1923: not said.
        Stop("cusco_qorikancha", "Qorikancha", "Qorikancha", -13.5203, -71.9752,
             note_en="The Incas' Temple of the Sun, once lined with gold; the Dominican convent was built on its walls.",
             note_it="Il Tempio del Sole degli Inca, un tempo rivestito d'oro; il convento domenicano fu costruito sulle sue mura."),
        # Source: Wikipedia, Coricancha; Wikipedia (es), Coricancha.
    ],
)

NEW_YORK = Walk(
    id="NEW_YORK_PARK_BRIDGE",
    city="new_york",
    city_en="New York",
    city_it="New York",
    route_en="From Central Park to the Brooklyn Bridge, by Times Square and the Empire State",
    route_it="Da Central Park al ponte di Brooklyn, passando per Times Square e l'Empire State",
    outing_en="A walk in New York",
    outing_it="Passeggiata a New York",
    country="US",
    continent="AMERICAS",
    # The Hudson and the East River are in the coastline: the sea comes up both sides of Manhattan.
    coast=True,
    # Central Park's Pond and Lake, and the two pools of the 9/11 Memorial.
    water=["way/22726524", "relation/7895705", "way/697722178", "way/697722181"],
    parks=[
        "way/427818536", "way/22727025", "way/22899286", "relation/7095444", "way/22899302", "way/413055246",
        "way/5029111", "relation/20812866",
    ],
    stops=[
        Stop("new_york_central_park", "Central Park", "Central Park", 40.7668, -73.9740,
             note_en="Frederick Law Olmsted and Calvert Vaux won the competition to design the park with their Greensward Plan.",
             note_it="Frederick Law Olmsted e Calvert Vaux vinsero il concorso per il parco con il loro Greensward Plan."),
        # Source: Wikipedia, Central Park; Wikipedia (it), Central Park. The year it opened differs between the two: neither is said.
        Stop("new_york_rockefeller_center", "Rockefeller Center", "Rockefeller Center", 40.7587, -73.9787,
             note_en="Every year a great Christmas tree rises here, above the skating rink opened in 1936.",
             note_it="Ogni anno qui si alza un grande albero di Natale, sopra la pista di pattinaggio aperta nel 1936."),
        # Source: Wikipedia, Rockefeller Center Christmas Tree; Wikipedia (it), Rockefeller Center.
        Stop("new_york_times_square", "Times Square", "Times Square", 40.7580, -73.9855,
             note_en="The square was named in 1904 after The New York Times, which had just moved here.",
             note_it="La piazza prese il nome nel 1904 dal New York Times, che vi si era appena trasferito."),
        # Source: Wikipedia, Times Square; Wikipedia (it), Times Square.
        Stop("new_york_public_library", "New York Public Library", "Biblioteca pubblica di New York", 40.7536, -73.9822,
             note_en="Two lions guard the library's steps: in the 1930s Mayor La Guardia named them Patience and Fortitude.",
             note_it="Due leoni custodiscono la scalinata della biblioteca: negli anni Trenta il sindaco La Guardia li chiamò Patience e Fortitude."),
        # Source: Wikipedia, New York Public Library Main Branch; Wikipedia (de), New York Public Library.
        Stop("new_york_grand_central", "Grand Central Terminal", "Grand Central Terminal", 40.7527, -73.9772,
             note_en="Opened in 1913, Grand Central has more platforms than any other station in the world.",
             note_it="Aperta nel 1913, la Grand Central ha più binari di qualunque altra stazione al mondo."),
        # Source: Wikipedia, Grand Central Terminal; Wikipedia (it), Grand Central Terminal.
        Stop("new_york_empire_state", "Empire State Building", "Empire State Building", 40.7484, -73.9857,
             note_en="Opened in 1931, it was then the tallest building in the world.",
             note_it="Inaugurato nel 1931, era allora l'edificio più alto del mondo."),
        # Source: Wikipedia, Empire State Building; Wikipedia (it), Empire State Building. How long it stayed the tallest differs: not said.
        Stop("new_york_flatiron", "Flatiron Building", "Flatiron Building", 40.7411, -73.9897,
             note_en="Since 1902 it has filled its triangular plot, where Fifth Avenue crosses Broadway.",
             note_it="Dal 1902 occupa il suo lotto triangolare, dove la Quinta Strada incrocia Broadway."),
        # Source: Wikipedia, Flatiron Building; Wikipedia (it), Flatiron Building.
        Stop("new_york_union_square", "Union Square", "Union Square", 40.7359, -73.9911,
             note_en="On 5 September 1882 the first Labor Day parade marched up Broadway to this square.",
             note_it="Il 5 settembre 1882 la prima parata del Labor Day risalì Broadway fino a questa piazza."),
        # Source: Wikipedia, Union Square, Manhattan; Wikipedia (it), Union Square (Manhattan).
        Stop("new_york_washington_square", "Washington Square", "Washington Square", 40.7308, -73.9973,
             note_en="Stanford White designed its arch for the centennial of George Washington's inauguration.",
             note_it="Stanford White ne disegnò l'arco per il centenario dell'insediamento di George Washington."),
        # Source: Wikipedia, Washington Square Arch; Wikipedia (de), Washington Square Arch.
        Stop("new_york_haughwout", "Haughwout Building", "Haughwout Building", 40.7222, -73.9992,
             note_en="In 1857 this cast-iron building had the world's first passenger elevator, by Elisha Otis.",
             note_it="Nel 1857 questo palazzo in ghisa ebbe il primo ascensore per persone del mondo, di Elisha Otis."),
        # Source: Wikipedia, E. V. Haughwout Building; Wikipedia (de), E. V. Haughwout Building.
        Stop("new_york_911_memorial", "9/11 Memorial", "Memoriale dell'11 settembre", 40.7115, -74.0134,
             note_en="Two pools fill the footprints of the Twin Towers, the names of the victims around their edges.",
             note_it="Due vasche occupano le impronte delle Torri Gemelle, con i nomi delle vittime lungo i bordi."),
        # Source: Wikipedia, National September 11 Memorial & Museum; Wikipedia (it), National September 11 Memorial & Museum.
        Stop("new_york_woolworth", "Woolworth Building", "Woolworth Building", 40.7124, -74.0080,
             note_en="When it opened in 1913, it was the tallest building in the world.",
             note_it="Quando fu inaugurato, nel 1913, era l'edificio più alto del mondo."),
        # Source: Wikipedia, Woolworth Building; Wikipedia (it), Woolworth Building.
        Stop("new_york_brooklyn_bridge", "Brooklyn Bridge", "Ponte di Brooklyn", 40.7106, -74.0026,
             note_en="Opened in 1883 across the East River, it was then the longest suspension bridge in the world.",
             note_it="Aperto nel 1883 sull'East River, era allora il ponte sospeso più lungo del mondo."),
        # Source: Wikipedia, Brooklyn Bridge; Wikipedia (it), Ponte di Brooklyn.
    ],
)

RIO = Walk(
    id="RIO_CENTRO_SUGARLOAF",
    city="rio",
    city_en="Rio de Janeiro",
    city_it="Rio de Janeiro",
    route_en="From the Museum of Tomorrow to the Sugarloaf, by Lapa and the bay",
    route_it="Dal Museo del Domani al Pan di Zucchero, passando per Lapa e la baia",
    outing_en="A walk in Rio",
    outing_it="Passeggiata a Rio",
    country="BR",
    continent="AMERICAS",
    # Guanabara Bay and the Atlantic, from the coastline.
    coast=True,
    # Flamengo Park, the Passeio Público, the Campo de Santana, and the natural monument of the
    # Sugarloaf and Urca hills, so the two hills show where the walk ends.
    parks=["relation/1124430", "way/64370326", "way/64370320", "way/1002850847"],
    stops=[
        Stop("rio_museum_of_tomorrow", "Museum of Tomorrow", "Museo del Domani", -22.8941, -43.1794,
             note_en="Santiago Calatrava's museum opened in 2015 on Pier Mauá, by the bay.",
             note_it="Il museo di Santiago Calatrava fu inaugurato nel 2015 sul molo Mauá, sulla baia."),
        # Source: Wikipedia, Museum of Tomorrow; Wikipedia (pt), Museu do Amanhã.
        Stop("rio_candelaria", "Candelária Church", "Chiesa della Candelária", -22.9008, -43.1774,
             note_en="Begun in 1775, the Candelária church had its dome only in 1877.",
             note_it="Iniziata nel 1775, la chiesa della Candelária ebbe la sua cupola solo nel 1877."),
        # Source: Wikipedia, Candelária Church; Wikipedia (pt), Igreja de Nossa Senhora da Candelária.
        Stop("rio_paco_imperial", "Imperial Palace", "Palazzo Imperiale", -22.9036, -43.1747,
             note_en="In this palace, in 1888, Princess Isabel signed the Golden Law that ended slavery in Brazil.",
             note_it="In questo palazzo, nel 1888, la principessa Isabella firmò la Legge Aurea che abolì la schiavitù in Brasile."),
        # Source: Wikipedia, Paço Imperial; Wikipedia (pt), Paço Imperial.
        Stop("rio_confeitaria_colombo", "Confeitaria Colombo", "Confeitaria Colombo", -22.9052, -43.1785,
             note_en="Founded in 1894, the café keeps the great crystal mirrors brought from Antwerp in the 1910s.",
             note_it="Fondata nel 1894, la pasticceria conserva i grandi specchi di cristallo arrivati da Anversa negli anni Dieci."),
        # Source: Wikipedia, Confeitaria Colombo; Wikipedia (pt), Confeitaria Colombo.
        Stop("rio_theatro_municipal", "Theatro Municipal", "Theatro Municipal", -22.9088, -43.1762,
             note_en="Opened in 1909, the theatre was inspired by Garnier's opera house in Paris.",
             note_it="Inaugurato nel 1909, il teatro si ispira all'Opéra di Garnier a Parigi."),
        # Source: Wikipedia, Theatro Municipal (Rio de Janeiro); Wikipedia (pt), Theatro Municipal do Rio de Janeiro.
        Stop("rio_arcos_da_lapa", "Lapa Arches", "Arcos da Lapa", -22.9128, -43.1800,
             note_en="Built as an aqueduct, the arches have carried the Santa Teresa tram since 1896.",
             note_it="Costruiti come acquedotto, gli archi portano dal 1896 il tram di Santa Teresa."),
        # Source: Wikipedia, Carioca Aqueduct; Wikipedia (pt), Arcos da Lapa.
        Stop("rio_selaron_steps", "Selarón Steps", "Scalinata Selarón", -22.9153, -43.1794,
             note_en="The Chilean artist Jorge Selarón covered these steps in tiles, first in the colours of Brazil's flag.",
             note_it="L'artista cileno Jorge Selarón ricoprì questi gradini di piastrelle, all'inizio nei colori della bandiera del Brasile."),
        # Source: Wikipedia, Escadaria Selarón; Wikipedia (pt), Escadaria Selarón.
        Stop("rio_gloria", "Outeiro da Glória", "Outeiro da Glória", -22.9213, -43.1752,
             note_en="Built on a plan of two octagons, this church saw the baptism of every member of Brazil's imperial family.",
             note_it="Costruita su una pianta di due ottagoni, questa chiesa vide il battesimo di tutti i membri della famiglia imperiale brasiliana."),
        # Source: Wikipedia (pt), Igreja de Nossa Senhora da Glória do Outeiro; Wikipedia (es) and
        # (fr), the same church. Its age (17th or 18th century) and who made its tiles differ: not said.
        Stop("rio_flamengo_park", "Flamengo Park", "Parco del Flamengo", -22.9340, -43.1742,
             note_en="Roberto Burle Marx, the great Brazilian landscape designer, laid out the gardens of this park by the bay.",
             note_it="Roberto Burle Marx, il grande paesaggista brasiliano, disegnò i giardini di questo parco sulla baia."),
        # Source: Wikipedia, Flamengo Park; Wikipedia (pt), Parque do Flamengo.
        Stop("rio_botafogo", "Botafogo Beach", "Spiaggia di Botafogo", -22.9440, -43.1820,
             note_en="Across the cove rises the Sugarloaf, 396 metres high.",
             note_it="Oltre l'insenatura si alza il Pan di Zucchero, alto 396 metri."),
        # Source: Wikipedia, Sugarloaf Mountain; Wikipedia (pt), Pão de Açúcar (Rio de Janeiro).
        Stop("rio_palacio_universitario", "Palácio Universitário", "Palácio Universitário", -22.9533, -43.1735,
             note_en="The palace was built for the Hospício Pedro II, the first psychiatric hospital in Brazil and the second in Latin America.",
             note_it="Il palazzo fu costruito per l'Hospício Pedro II, il primo ospedale psichiatrico del Brasile e il secondo dell'America Latina."),
        # Source: Wikipedia (pt), Hospício Pedro II; Wikipedia, Legacy of Pedro II of Brazil.
        Stop("rio_sugarloaf", "Sugarloaf cable car", "Funivia del Pan di Zucchero", -22.9549, -43.1664,
             note_en="Opened in 1912, the cable car climbs from here to Urca Hill, then on to the Sugarloaf.",
             note_it="Inaugurata nel 1912, la funivia sale da qui al Morro da Urca, poi al Pan di Zucchero."),
        # Source: Wikipedia, Sugarloaf Cable Car; Wikipedia (pt), Bondinho do Pão de Açúcar.
    ],
)

MEXICO_CITY = Walk(
    id="MEXICO_CITY_ZOCALO_CHAPULTEPEC",
    city="mexico_city",
    city_en="Mexico City",
    city_it="Città del Messico",
    route_en="From the Zócalo to Chapultepec, along the Paseo de la Reforma",
    route_it="Dallo Zócalo a Chapultepec, lungo il Paseo de la Reforma",
    outing_en="A walk in Mexico City",
    outing_it="Passeggiata a Città del Messico",
    country="MX",
    continent="AMERICAS",
    # Chapultepec's lake; its forest is mapped as many woods, of which the largest are drawn.
    water=["relation/16031520"],
    parks=[
        "way/4758957", "way/1356340885", "way/1356340884", "way/1356340882", "way/1356049208",
        "way/1356049216", "way/1356049205", "way/1356049204", "way/1356049213",
    ],
    stops=[
        Stop("mexico_palacio_nacional", "National Palace", "Palazzo Nazionale", 19.4326, -99.1313,
             note_en="Diego Rivera's murals of Mexico's history cover the main stairway of the National Palace.",
             note_it="I murales di Diego Rivera sulla storia del Messico coprono lo scalone del Palazzo Nazionale."),
        # Source: Wikipedia, National Palace (Mexico); Wikipedia (es), Palacio Nacional (México).
        Stop("mexico_templo_mayor", "Templo Mayor", "Templo Mayor", 19.435, -99.1318,
             note_en="The Aztecs' great temple came to light again in 1978, found by electricity workers digging in the street.",
             note_it="Il grande tempio degli Aztechi tornò alla luce nel 1978, trovato da operai della compagnia elettrica che scavavano in strada."),
        # Source: Wikipedia, Templo Mayor; Wikipedia (es), Templo Mayor. The two name different electricity companies: neither is said.
        Stop("mexico_cathedral", "Metropolitan Cathedral", "Cattedrale metropolitana", 19.4339, -99.1332,
             note_en="Building it took from 1573 to 1813, around the church that stood here first.",
             note_it="Costruirla richiese dal 1573 al 1813, intorno alla chiesa che sorgeva qui prima."),
        # Source: Wikipedia, Mexico City Metropolitan Cathedral; Wikipedia (es), Catedral Metropolitana de la Ciudad de México.
        Stop("mexico_casa_azulejos", "House of Tiles", "Casa de los Azulejos", 19.4342, -99.1398,
             note_en="Its façade is covered in Talavera tiles from Puebla, which gave the house its name.",
             note_it="La facciata è coperta di piastrelle di Talavera di Puebla, che hanno dato il nome alla casa."),
        # Source: Wikipedia, Casa de los Azulejos; Wikipedia (es), Casa de los Azulejos.
        Stop("mexico_bellas_artes", "Palace of Fine Arts", "Palazzo delle Belle Arti", 19.4353, -99.141,
             note_en="Begun in 1904 for the centenary of independence, it opened only in 1934, after the Revolution.",
             note_it="Iniziato nel 1904 per il centenario dell'indipendenza, fu inaugurato solo nel 1934, dopo la Rivoluzione."),
        # Source: Wikipedia, Palacio de Bellas Artes; Wikipedia (es), Palacio de Bellas Artes (México).
        Stop("mexico_alameda", "Alameda Central", "Alameda Central", 19.4357, -99.144,
             note_en="Laid out in 1592, it is the oldest public park in the Americas.",
             note_it="Creata nel 1592, è il più antico parco pubblico delle Americhe."),
        # Source: Wikipedia, Alameda Central; Wikipedia (es), Alameda Central.
        Stop("mexico_revolucion", "Monument to the Revolution", "Monumento alla Rivoluzione", 19.4361, -99.1546,
             note_en="It was built from the frame of a legislative palace that was never finished.",
             note_it="Fu costruito con la struttura di un palazzo legislativo mai terminato."),
        # Source: Wikipedia, Monument to the Revolution (Mexico City); Wikipedia (es), Monumento a la Revolución (México).
        Stop("mexico_angel", "Angel of Independence", "Angelo dell'Indipendenza", 19.427, -99.1677,
             note_en="Inaugurated in 1910 for the centenary of independence, its angel fell in the earthquake of 1957.",
             note_it="Inaugurato nel 1910 per il centenario dell'indipendenza, il suo angelo cadde nel terremoto del 1957."),
        # Source: Wikipedia, Angel of Independence; Wikipedia (es), Ángel de la Independencia.
        Stop("mexico_diana", "Diana the Huntress", "Diana Cacciatrice", 19.4251, -99.1716,
             note_en="The bronze huntress, unveiled in 1942, aims her arrow at the stars of the northern sky.",
             note_it="La cacciatrice di bronzo, inaugurata nel 1942, punta la freccia verso le stelle del cielo del nord."),
        # Source: Wikipedia, Diana the Huntress Fountain; Wikipedia (es), Fuente de la Diana Cazadora (Ciudad de México).
        Stop("mexico_ninos_heroes", "Monument to the Boy Heroes", "Monumento ai Niños Héroes", 19.4215, -99.1793,
             note_en="It remembers the young cadets who died defending Chapultepec Castle in 1847.",
             note_it="Ricorda i giovani cadetti morti difendendo il castello di Chapultepec nel 1847."),
        # Source: Wikipedia, Niños Héroes; Wikipedia (es), Niños Héroes.
        Stop("mexico_chapultepec_castle", "Chapultepec Castle", "Castello di Chapultepec", 19.4205, -99.182,
             note_en="Emperor Maximilian and Empress Carlota made this castle on its hill their residence.",
             note_it="L'imperatore Massimiliano e l'imperatrice Carlotta fecero di questo castello sulla collina la loro residenza."),
        # Source: Wikipedia, Chapultepec Castle; Wikipedia (es), Castillo de Chapultepec.
        Stop("mexico_anthropology", "Museum of Anthropology", "Museo di Antropologia", 19.4261, -99.1863,
             note_en="Its heart is the Aztec Sun Stone, found under the Zócalo in 1790.",
             note_it="Il suo cuore è la Pietra del Sole azteca, ritrovata sotto lo Zócalo nel 1790."),
        # Source: Wikipedia, National Museum of Anthropology (Mexico) and Aztec sun stone; Wikipedia (es), Museo Nacional de Antropología (México) and Piedra del Sol.
    ],
)

BUENOS_AIRES = Walk(
    id="BUENOS_AIRES_MAYO_RECOLETA",
    city="buenos_aires",
    city_en="Buenos Aires",
    city_it="Buenos Aires",
    route_en="From the Plaza de Mayo to Recoleta, by the Congress and the Obelisco",
    route_it="Da Plaza de Mayo alla Recoleta, passando per il Congresso e l'Obelisco",
    outing_en="A walk in Buenos Aires",
    outing_it="Passeggiata a Buenos Aires",
    country="AR",
    continent="AMERICAS",
    # The Río de la Plata, from the coastline; Puerto Madero's docks.
    coast=True,
    # The grid of the centre is mapped mostly as residential streets: its longer ones are drawn,
    # as Milan's are, or the map would show only the avenues.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    water=["relation/2364166", "relation/2364163", "relation/2364162"],
    # The squares on the way, and the Costanera Sur's nature reserve by the river.
    parks=[
        "relation/17076039", "way/17493172", "relation/531073", "way/23620740", "way/23727108",
        "relation/10343154",
    ],
    stops=[
        Stop("buenos_aires_casa_rosada", "Casa Rosada", "Casa Rosada", -34.6081, -58.371,
             note_en="The president's palace takes its name from its pink colour.",
             note_it="Il palazzo del presidente prende il nome dal suo colore rosa."),
        # Source: Wikipedia, Casa Rosada; Wikipedia (es), Casa Rosada.
        Stop("buenos_aires_cathedral", "Metropolitan Cathedral", "Cattedrale metropolitana", -34.6075, -58.3733,
             note_en="General José de San Martín, hero of independence, rests in a mausoleum inside the cathedral.",
             note_it="Il generale José de San Martín, eroe dell'indipendenza, riposa in un mausoleo dentro la cattedrale."),
        # Source: Wikipedia, Buenos Aires Metropolitan Cathedral; Wikipedia (es), Catedral metropolitana de Buenos Aires.
        Stop("buenos_aires_cabildo", "Cabildo", "Cabildo", -34.6087, -58.374,
             note_en="The colonial town hall, begun in 1725, is now the museum of the May Revolution of 1810.",
             note_it="Il municipio coloniale, iniziato nel 1725, è oggi il museo della Rivoluzione di Maggio del 1810."),
        # Source: Wikipedia, Cabildo of Buenos Aires; Wikipedia (es), Cabildo de Buenos Aires.
        Stop("buenos_aires_tortoni", "Café Tortoni", "Café Tortoni", -34.6087, -58.3781,
             note_en="Opened in 1858, the café counted Jorge Luis Borges among its regulars.",
             note_it="Aperto nel 1858, il caffè ebbe tra i suoi frequentatori Jorge Luis Borges."),
        # Source: Wikipedia, Café Tortoni; Wikipedia (es), Café Tortoni.
        Stop("buenos_aires_barolo", "Palacio Barolo", "Palacio Barolo", -34.6094, -58.3856,
             note_en="Its design follows Dante's Divine Comedy: 100 metres tall, one for each canto.",
             note_it="Il suo progetto segue la Divina Commedia di Dante: alto 100 metri, uno per ogni canto."),
        # Source: Wikipedia, Palacio Barolo; Wikipedia (es), Palacio Barolo.
        Stop("buenos_aires_congreso", "Congress", "Congresso", -34.6097, -58.3921,
             note_en="Designed by Vittorio Meano, the Congress palace was inaugurated in 1906.",
             note_it="Progettato da Vittorio Meano, il palazzo del Congresso fu inaugurato nel 1906."),
        # Source: Wikipedia, Argentine National Congress Palace; Wikipedia (es), Palacio del Congreso de la Nación Argentina.
        Stop("buenos_aires_obelisco", "Obelisco", "Obelisco", -34.6037, -58.3816,
             note_en="Raised in 1936, it marks four hundred years since the city's first founding.",
             note_it="Eretto nel 1936, ricorda i quattrocento anni dalla prima fondazione della città."),
        # Source: Wikipedia, Obelisco de Buenos Aires; Wikipedia (es), Obelisco de Buenos Aires.
        Stop("buenos_aires_colon", "Teatro Colón", "Teatro Colón", -34.6011, -58.383,
             note_en="A survey of conductors by Leo Beranek ranked its hall the best in the world for opera.",
             note_it="Un sondaggio di Leo Beranek tra i direttori ne ha giudicato la sala la migliore al mondo per l'opera."),
        # Source: Wikipedia, Teatro Colón; Wikipedia (es), Teatro Colón.
        Stop("buenos_aires_plaza_san_martin", "Plaza San Martín", "Plaza San Martín", -34.595, -58.3755,
             note_en="The square is named after General San Martín, whose equestrian statue stands here.",
             note_it="La piazza porta il nome del generale San Martín, la cui statua equestre si trova qui."),
        # Source: Wikipedia, Plaza San Martín (Buenos Aires); Wikipedia (es), Plaza San Martín (Buenos Aires).
        Stop("buenos_aires_ateneo", "El Ateneo Grand Splendid", "El Ateneo Grand Splendid", -34.596, -58.3943,
             note_en="The Grand Splendid theatre of 1919 is now a bookshop, with tables on its old stage.",
             note_it="Il teatro Grand Splendid del 1919 è oggi una libreria, con i tavoli sul vecchio palcoscenico."),
        # Source: Wikipedia, El Ateneo Grand Splendid; Wikipedia (es), El Ateneo Grand Splendid.
        Stop("buenos_aires_recoleta", "Recoleta Cemetery", "Cimitero della Recoleta", -34.588, -58.3926,
             note_en="Opened in 1822, the cemetery holds the tombs of presidents and of Eva Perón.",
             note_it="Aperto nel 1822, il cimitero custodisce le tombe di presidenti e di Eva Perón."),
        # Source: Wikipedia, La Recoleta Cemetery; Wikipedia (es), Cementerio de la Recoleta.
        Stop("buenos_aires_bellas_artes", "Museum of Fine Arts", "Museo di Belle Arti", -34.5839, -58.3929,
             note_en="Founded in 1895, the national museum of fine arts now fills an old waterworks pump house.",
             note_it="Fondato nel 1895, il museo nazionale di belle arti occupa oggi un'antica stazione di pompaggio."),
        # Source: Wikipedia, Museo Nacional de Bellas Artes (Buenos Aires); Wikipedia (es), Museo Nacional de Bellas Artes (Argentina).
        Stop("buenos_aires_floralis", "Floralis Genérica", "Floralis Genérica", -34.5817, -58.3935,
             note_en="Eduardo Catalano's metal flower, 23 metres tall, was made to close its six petals at night.",
             note_it="Il fiore di metallo di Eduardo Catalano, alto 23 metri, fu fatto per chiudere di notte i suoi sei petali."),
        # Source: Wikipedia, Floralis Genérica; Wikipedia (es), Floralis Genérica.
    ],
)

SAN_FRANCISCO = Walk(
    id="SAN_FRANCISCO_FERRY_PALACE",
    city="san_francisco",
    city_en="San Francisco",
    city_it="San Francisco",
    route_en="From the Ferry Building to the Palace of Fine Arts, by Coit Tower and the Wharf",
    route_it="Dal Ferry Building al Palace of Fine Arts, passando per la Coit Tower e il Wharf",
    outing_en="A walk in San Francisco",
    outing_it="Passeggiata a San Francisco",
    country="US",
    continent="AMERICAS",
    # The bay, from the coastline; the lagoon of the Palace of Fine Arts.
    coast=True,
    water=["relation/7471537"],
    parks=[
        "way/18583270", "way/224941774", "way/82207054", "way/16761472", "relation/8346137",
    ],
    stops=[
        Stop("sf_ferry_building", "Ferry Building", "Ferry Building", 37.7955, -122.3937,
             note_en="The clock tower of this ferry terminal may have been modelled on the Giralda of Seville.",
             note_it="La torre dell'orologio di questo terminal dei traghetti potrebbe ispirarsi alla Giralda di Siviglia."),
        # Source: Wikipedia, San Francisco Ferry Building; Wikipedia (es), San Francisco Ferry Building. Both say "may have": so does the sentence.
        Stop("sf_transamerica", "Transamerica Pyramid", "Transamerica Pyramid", 37.7952, -122.4028,
             note_en="On its completion in 1972 the pyramid became the tallest building in San Francisco.",
             note_it="Completata nel 1972, la piramide divenne l'edificio più alto di San Francisco."),
        # Source: Wikipedia, Transamerica Pyramid (tallest from 1972 until 2017); Wikipedia (it), Transamerica Pyramid.
        Stop("sf_chinatown", "Chinatown Gate", "Porta di Chinatown", 37.7907, -122.4056,
             note_en="Through the Dragon Gate, Grant Avenue leads into the oldest Chinatown in North America.",
             note_it="Oltre la Porta del Drago, Grant Avenue entra nella più antica Chinatown del Nord America."),
        # Source: Wikipedia, Chinatown, San Francisco; Wikipedia (it), Chinatown (San Francisco).
        Stop("sf_city_lights", "City Lights", "City Lights", 37.7976, -122.4065,
             note_en="Founded in 1953 by Lawrence Ferlinghetti, the bookshop published Allen Ginsberg's Howl in 1956.",
             note_it="Fondata nel 1953 da Lawrence Ferlinghetti, la libreria pubblicò Howl di Allen Ginsberg nel 1956."),
        # Source: Wikipedia, City Lights Booksellers & Publishers; Wikipedia (it), City Lights Bookstore.
        Stop("sf_saints_peter_paul", "Saints Peter and Paul Church", "Chiesa dei Santi Pietro e Paolo", 37.8013, -122.4098,
             note_en="Marilyn Monroe and Joe DiMaggio posed for photographs on the steps of this church.",
             note_it="Marilyn Monroe e Joe DiMaggio posarono per i fotografi sui gradini di questa chiesa."),
        # Source: Wikipedia, Saints Peter and Paul Church (San Francisco); Wikipedia (it), Chiesa dei Santi Pietro e Paolo (San Francisco).
        Stop("sf_coit_tower", "Coit Tower", "Coit Tower", 37.8024, -122.4058,
             note_en="Built with the bequest of Lillie Hitchcock Coit, the tower is painted inside with murals by many artists.",
             note_it="Costruita con il lascito di Lillie Hitchcock Coit, la torre è dipinta all'interno con murales di molti artisti."),
        # Source: Wikipedia, Coit Tower; Wikipedia (it), Coit Tower. Their years and number of artists differ: not said.
        Stop("sf_pier_39", "Pier 39", "Pier 39", 37.8087, -122.4098,
             note_en="Since 1989 a colony of sea lions has rested on the docks of this pier's marina.",
             note_it="Dal 1989 una colonia di leoni marini riposa sui pontili del porticciolo di questo molo."),
        # Source: Wikipedia, Pier 39; Wikipedia (it), Pier 39.
        Stop("sf_fishermans_wharf", "Fisherman's Wharf", "Fisherman's Wharf", 37.8081, -122.4166,
             note_en="The wharf is home to the San Francisco Maritime National Historical Park and its old ships.",
             note_it="Il molo ospita il parco storico marittimo nazionale di San Francisco e le sue vecchie navi."),
        # Source: Wikipedia, Fisherman's Wharf, San Francisco; Wikipedia (es), Fisherman's Wharf (San Francisco).
        Stop("sf_lombard", "Lombard Street", "Lombard Street", 37.8021, -122.4187,
             note_en="Between Hyde and Leavenworth, the street winds down Russian Hill in tight hairpin bends.",
             note_it="Tra Hyde e Leavenworth, la strada scende dalla Russian Hill in stretti tornanti."),
        # Source: Wikipedia, Lombard Street (San Francisco); Wikipedia (it), Lombard Street (San Francisco).
        Stop("sf_ghirardelli", "Ghirardelli Square", "Ghirardelli Square", 37.8059, -122.4229,
             note_en="Once Ghirardelli's chocolate factory, it opened as a square of shops in 1964.",
             note_it="Un tempo fabbrica di cioccolato Ghirardelli, nel 1964 divenne una piazza di negozi."),
        # Source: Wikipedia, Ghirardelli Square; Wikipedia (it), Ghirardelli Square and Fisherman's Wharf.
        Stop("sf_fort_mason", "Fort Mason", "Fort Mason", 37.8063, -122.429,
             note_en="In the Second World War this was the main port for the war in the Pacific.",
             note_it="Nella seconda guerra mondiale fu il porto principale per la guerra nel Pacifico."),
        # Source: Wikipedia, Fort Mason; Wikipedia (de), Fort Mason.
        Stop("sf_palace_fine_arts", "Palace of Fine Arts", "Palace of Fine Arts", 37.8029, -122.4484,
             note_en="Built for the Panama–Pacific Exposition of 1915, it was rebuilt from the 1960s.",
             note_it="Costruito per l'Esposizione Panama-Pacifico del 1915, fu ricostruito a partire dagli anni Sessanta."),
        # Source: Wikipedia, Palace of Fine Arts; Wikipedia (it), Palace of Fine Arts.
    ],
)

QUEBEC = Walk(
    id="QUEBEC_PARLEMENT_BASSE_VILLE",
    city="quebec",
    city_en="Québec",
    city_it="Québec",
    route_en="From the Parliament to the Lower Town, by the Plains of Abraham and the Château Frontenac",
    route_it="Dal Parlamento alla Città Bassa, passando per le Piane di Abramo e il Château Frontenac",
    outing_en="A walk in Québec",
    outing_it="Passeggiata a Québec",
    country="CA",
    continent="AMERICAS",
    # A short walk (about 5 km). At Québec the St Lawrence is not yet coastline in OpenStreetMap
    # but a river area, the fluvial estuary; with it, the Louise Basin of the Old Port.
    water=["relation/2426031", "relation/5869253"],
    # The Upper Town's streets are mostly residential: without them the map is a few lines.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    parks=["relation/21295717", "relation/21295715", "way/105924778", "way/105924793", "way/107619990"],
    stops=[
        Stop("quebec_parlement", "Parliament Building", "Palazzo del Parlamento", 46.8087, -71.2142,
             note_en="Statues along its façade tell the history of Québec.",
             note_it="Le statue lungo la facciata raccontano la storia del Québec."),
        # Source: Wikipedia, Parliament Building (Quebec); Wikipedia (fr), Hôtel du Parlement du Québec.
        Stop("quebec_plaines", "Plains of Abraham", "Piane di Abramo", 46.802, -71.218,
             note_en="Here in 1759 the British won the battle for Québec; both generals, Wolfe and Montcalm, died of their wounds.",
             note_it="Qui nel 1759 gli inglesi vinsero la battaglia per il Québec; entrambi i generali, Wolfe e Montcalm, morirono per le ferite."),
        # Source: Wikipedia, Plains of Abraham; Wikipedia (fr), Plaines d'Abraham.
        Stop("quebec_citadelle", "Citadelle", "Cittadella", 46.8077, -71.2078,
             note_en="The British built this star-shaped fortress from 1820, under the engineer Elias Walker Durnford.",
             note_it="Gli inglesi costruirono questa fortezza a stella dal 1820, sotto la guida dell'ingegnere Elias Walker Durnford."),
        # Source: Wikipedia, Citadelle of Quebec; Wikipedia (fr), Citadelle de Québec.
        Stop("quebec_frontenac", "Château Frontenac", "Château Frontenac", 46.8115, -71.2044,
             note_en="The hotel on the cliff is called the most photographed in the world.",
             note_it="L'albergo sulla rupe è detto il più fotografato del mondo."),
        # Source: Wikipedia, Château Frontenac; Wikipedia (fr), Château Frontenac.
        Stop("quebec_notre_dame", "Notre-Dame de Québec", "Notre-Dame de Québec", 46.8137, -71.2061,
             note_en="Seat of the oldest diocese north of Mexico, it was made a basilica by Pius IX in 1874.",
             note_it="Sede della più antica diocesi a nord del Messico, fu elevata a basilica da Pio IX nel 1874."),
        # Source: Wikipedia, Cathedral Basilica of Notre-Dame de Québec; Wikipedia (fr), Basilique-cathédrale Notre-Dame de Québec.
        Stop("quebec_casse_cou", "Breakneck Stairs", "Escalier Casse-Cou", 46.8128, -71.2036,
             note_en="Québec's oldest stairway joins the Upper Town to the Lower; it was already here in 1660.",
             note_it="La scala più antica di Québec unisce la Città Alta alla Bassa; esisteva già nel 1660."),
        # Source: Wikipedia, Breakneck Stairs (built 1635); Wikipedia (fr), Quartier Petit Champlain (there in 1660). The year it was built is in one only: not said.
        Stop("quebec_place_royale", "Place Royale", "Place Royale", 46.8131, -71.2027,
             note_en="Here in 1608 Samuel de Champlain built the fortified post that became Québec.",
             note_it="Qui nel 1608 Samuel de Champlain costruì il posto fortificato da cui nacque Québec."),
        # Source: Wikipedia, Place Royale (Quebec City); Wikipedia (fr), Place Royale (Québec).
        Stop("quebec_musee_civilisation", "Musée de la civilisation", "Musée de la civilisation", 46.8152, -71.2023,
             note_en="Designed by Moshe Safdie, the museum opened in 1988 by the St Lawrence.",
             note_it="Progettato da Moshe Safdie, il museo aprì nel 1988 sulla riva del San Lorenzo."),
        # Source: Wikipedia, Musée de la civilisation; Wikipedia (fr), Musée de la civilisation.
    ],
)

HAVANA = Walk(
    id="HAVANA_CAPITOLIO_PAULA",
    city="havana",
    city_en="Havana",
    city_it="L'Avana",
    route_en="From the Capitolio to the Alameda de Paula, by the Malecón and the old squares",
    route_it="Dal Capitolio all'Alameda de Paula, passando per il Malecón e le piazze antiche",
    outing_en="A walk in Havana",
    outing_it="Passeggiata all'Avana",
    country="CU",
    continent="AMERICAS",
    # A short walk (about 5 km). The sea and the harbour, from the coastline.
    coast=True,
    water=[],
    parks=["way/23872622"],
    stops=[
        Stop("havana_capitolio", "Capitolio", "Capitolio", 23.1353, -82.3597,
             note_en="A replica diamond set in the floor of its hall marks kilometre zero of Cuba's roads.",
             note_it="Una replica di diamante incastonata nel pavimento del salone segna il chilometro zero delle strade cubane."),
        # Source: Wikipedia, El Capitolio; Wikipedia (es), Capitolio Nacional de Cuba.
        Stop("havana_gran_teatro", "Gran Teatro", "Gran Teatro", 23.1369, -82.3596,
             note_en="Inaugurated in 1914, it stands where the Teatro Tacón stood before it.",
             note_it="Inaugurato nel 1914, sorge dove prima c'era il Teatro Tacón."),
        # Source: Wikipedia, Gran Teatro de La Habana; Wikipedia (es), Gran Teatro de La Habana Alicia Alonso.
        Stop("havana_prado", "Paseo del Prado", "Paseo del Prado", 23.142, -82.3585,
             note_en="Bronze lions guard this promenade, which runs down to the Malecón.",
             note_it="Leoni di bronzo custodiscono questo viale, che scende fino al Malecón."),
        # Source: Wikipedia, Paseo del Prado, Havana; Wikipedia (es), Paseo del Prado (La Habana).
        Stop("havana_la_punta", "Castillo de la Punta", "Castillo de la Punta", 23.1462, -82.3575,
             note_en="From 1630 a heavy chain stretched from this castle to El Morro guarded the bay.",
             note_it="Dal 1630 una pesante catena tesa da questo castello a El Morro proteggeva la baia."),
        # Source: Wikipedia, Castillo de San Salvador de la Punta; Wikipedia (es), Castillo de San Salvador de la Punta.
        Stop("havana_cathedral", "Havana Cathedral", "Cattedrale dell'Avana", 23.1417, -82.352,
             note_en="Columbus's remains lay in this cathedral until 1898, when they were taken to Seville.",
             note_it="I resti di Colombo riposarono in questa cattedrale fino al 1898, quando furono portati a Siviglia."),
        # Source: Wikipedia, Havana Cathedral; Wikipedia (es), Catedral de La Habana. When they arrived differs (1795 or 1796): not said.
        Stop("havana_real_fuerza", "Castillo de la Real Fuerza", "Castillo de la Real Fuerza", 23.1411, -82.3496,
             note_en="One of the oldest stone forts in the Americas, it wears the Giraldilla weathervane on its tower.",
             note_it="Uno dei più antichi forti di pietra delle Americhe, porta sulla torre la banderuola della Giraldilla."),
        # Source: Wikipedia, Castillo de la Real Fuerza; Wikipedia (es), Castillo de la Real Fuerza de La Habana.
        Stop("havana_plaza_armas", "Plaza de Armas", "Plaza de Armas", 23.1402, -82.3496,
             note_en="This is the oldest square in Old Havana.",
             note_it="È la piazza più antica dell'Avana Vecchia."),
        # Source: Wikipedia, Plaza de Armas (Havana); Wikipedia (es), Plaza de Armas (La Habana).
        Stop("havana_san_francisco", "Plaza de San Francisco", "Plaza de San Francisco", 23.1378, -82.3487,
             note_en="The basilica on this square, begun in 1548, is now a concert hall.",
             note_it="La basilica su questa piazza, iniziata nel 1548, è oggi una sala da concerto."),
        # Source: Wikipedia, Basilica of San Francisco de Asís, Havana; Wikipedia (es), Convento de San Francisco de Asís (La Habana).
        Stop("havana_plaza_vieja", "Plaza Vieja", "Plaza Vieja", 23.1361, -82.35,
             note_en="Laid out in 1559, it was first called the Plaza Nueva, the New Square.",
             note_it="Nata nel 1559, all'inizio si chiamava Plaza Nueva, la Piazza Nuova."),
        # Source: Wikipedia, Plaza Vieja, Havana; Wikipedia (es), Plaza Vieja (La Habana).
        Stop("havana_alameda_paula", "Alameda de Paula", "Alameda de Paula", 23.132, -82.3481,
             note_en="Built in 1777, it was the city's first promenade.",
             note_it="Costruita nel 1777, fu la prima passeggiata della città."),
        # Source: Wikipedia, Alameda de Paula; Wikipedia (es), Alameda de Paula.
    ],
)

CARTAGENA = Walk(
    id="CARTAGENA_RELOJ_SAN_FELIPE",
    city="cartagena",
    city_en="Cartagena",
    city_it="Cartagena",
    route_en="From the Clock Tower to San Felipe, along the walls and through Getsemaní",
    route_it="Dalla Torre dell'Orologio a San Felipe, lungo le mura e attraverso Getsemaní",
    outing_en="A walk in Cartagena",
    outing_it="Passeggiata a Cartagena",
    country="CO",
    continent="AMERICAS",
    # A short walk (about 5 km). The Caribbean and the bay, from the coastline; the Chambacú lagoon.
    coast=True,
    water=["way/238972766"],
    parks=["way/25841728", "way/25447599", "way/49600262"],
    stops=[
        Stop("cartagena_torre_reloj", "Clock Tower", "Torre dell'Orologio", 10.4227, -75.5488,
             note_en="The main gate of the walled city takes its name from the clock set on it in the 18th century.",
             note_it="La porta principale della città murata prende il nome dall'orologio posto in cima nel Settecento."),
        # Source: Wikipedia, Puerta del Reloj; Wikipedia (es), Torre del Reloj (Cartagena de Indias).
        Stop("cartagena_santo_domingo", "Plaza de Santo Domingo", "Plaza de Santo Domingo", 10.4243, -75.552,
             note_en="The church of Santo Domingo, on this square, was built between about 1565 and 1630.",
             note_it="La chiesa di Santo Domingo, su questa piazza, fu costruita tra il 1565 circa e il 1630."),
        # Source: Wikipedia, Convento de Santo Domingo, Cartagena; Wikipedia (es), Convento de Santo Domingo (Cartagena).
        Stop("cartagena_cathedral", "Cathedral", "Cattedrale", 10.4237, -75.5507,
             note_en="In 1586, still unfinished, the cathedral was damaged in Francis Drake's attack on the city.",
             note_it="Nel 1586, ancora incompiuta, la cattedrale fu danneggiata nell'attacco di Francis Drake alla città."),
        # Source: Wikipedia, Cartagena Cathedral, Colombia; Wikipedia (es), Catedral de Santa Catalina de Alejandría (Cartagena de Indias).
        Stop("cartagena_inquisicion", "Palace of the Inquisition", "Palazzo dell'Inquisizione", 10.4232, -75.5516,
             note_en="This palace, finished in 1770, was the seat of the Inquisition's tribunal in Cartagena.",
             note_it="Questo palazzo, finito nel 1770, fu la sede del tribunale dell'Inquisizione a Cartagena."),
        # Source: Wikipedia, Palace of the Inquisition (Cartagena, Colombia); Wikipedia (es), Palacio de la Inquisición (Cartagena de Indias).
        Stop("cartagena_murallas", "City walls", "Mura", 10.4262, -75.5527,
             note_en="Begun in 1614, the walls were built in stages to protect the city from pirates.",
             note_it="Iniziate nel 1614, le mura furono costruite a più riprese per difendere la città dai pirati."),
        # Source: Wikipedia, Cartagena, Colombia (built between 1614 and 1796); Wikipedia (es), Cartagena de Indias.
        Stop("cartagena_bovedas", "Las Bóvedas", "Las Bóvedas", 10.43, -75.5465,
             note_en="Twenty-three vaults in the walls, built as storerooms and later used as prison cells.",
             note_it="Ventitré volte nelle mura, costruite come magazzini e poi usate come celle di prigione."),
        # Source: Wikipedia, Las Bóvedas; Wikipedia (es), Cuartel de Las Bóvedas.
        Stop("cartagena_trinidad", "Plaza de la Trinidad", "Plaza de la Trinidad", 10.4206, -75.5454,
             note_en="On 11 November 1811 the people of Getsemaní, with Pedro Romero, pushed Cartagena to declare its independence.",
             note_it="L'11 novembre 1811 la gente di Getsemaní, con Pedro Romero, spinse Cartagena a dichiarare l'indipendenza."),
        # Source: Wikipedia, Cartagena, Colombia; Wikipedia (es), Cartagena de Indias.
        Stop("cartagena_san_felipe", "Castillo San Felipe", "Castillo San Felipe", 10.4227, -75.5394,
             note_en="In 1741 its defenders, under Blas de Lezo, held off the British fleet of Admiral Vernon.",
             note_it="Nel 1741 i suoi difensori, al comando di Blas de Lezo, respinsero la flotta inglese dell'ammiraglio Vernon."),
        # Source: Wikipedia, Castillo San Felipe de Barajas; Wikipedia (es), Castillo San Felipe de Barajas.
    ],
)

TOKYO = Walk(
    id="TOKYO_SENSOJI_PALACE",
    city="tokyo",
    city_en="Tokyo",
    city_it="Tokyo",
    route_en="From Sensō-ji to the Imperial Palace, by Ueno and Akihabara",
    route_it="Dal Sensō-ji al Palazzo imperiale, passando per Ueno e Akihabara",
    outing_en="A walk in Tokyo",
    outing_it="Passeggiata a Tokyo",
    country="JP",
    continent="ASIA_OCEANIA",
    # The Sumida, in several areas; the Kanda; the palace's moats; Shinobazu Pond. The Sumida's
    # banks and the Nihonbashi River are also many small areas, read from the street tiles.
    water=[
        "relation/14352161", "relation/8284278", "relation/12489063", "relation/7913942", "relation/12489064",
        "relation/3553642",
        "relation/5415353", "relation/5415354", "relation/5415355", "relation/5415347", "relation/5415372",
        "relation/5415371", "relation/5415373", "relation/5415352", "relation/5415356", "relation/3682169",
        "relation/5414261", "relation/7904532",
    ],
    water_from_tiles=True,
    # Ueno Park, the palace's gardens and woods, Kitanomaru, Hibiya and the Sumida's park.
    parks=[
        "relation/5413419", "relation/3551852", "relation/5415394", "relation/2102944", "way/624081603",
        "way/145408909", "relation/14235095",
    ],
    stops=[
        Stop("tokyo_kaminarimon", "Kaminarimon", "Kaminarimon", 35.7111, 139.7964,
             note_en="Guarded by the gods of wind and thunder, this gate was rebuilt in 1960 with a gift from Panasonic's founder, Kōnosuke Matsushita.",
             note_it="Custodita dagli dei del vento e del tuono, questa porta fu ricostruita nel 1960 con un dono del fondatore di Panasonic, Kōnosuke Matsushita."),
        # Source: Wikipedia, Kaminarimon; Wikipedia (ja), 雷門. The first gate's year differs (941 or 942): not said.
        Stop("tokyo_sensoji", "Sensō-ji", "Sensō-ji", 35.7146, 139.7966,
             note_en="Tokyo's oldest temple began, the legend says, with a statue of Kannon two brothers found while fishing in the Sumida in 628.",
             note_it="Il tempio più antico di Tokyo nacque, dice la leggenda, da una statua di Kannon trovata da due fratelli mentre pescavano nel Sumida nel 628."),
        # Source: Wikipedia, Sensō-ji; Wikipedia (it), Sensō-ji.
        Stop("tokyo_kappabashi", "Kappabashi", "Kappabashi", 35.7135, 139.788,
             note_en="Between Asakusa and Ueno, this street sells restaurants everything from knives to the display food in their windows.",
             note_it="Tra Asakusa e Ueno, questa via vende ai ristoranti di tutto, dai coltelli ai piatti finti esposti in vetrina."),
        # Source: Wikipedia, Kappabashi-dori; Wikipedia (ja), かっぱ橋道具街.
        Stop("tokyo_national_museum", "Tokyo National Museum", "Museo nazionale di Tokyo", 35.718, 139.7765,
             note_en="Japan's oldest national museum, founded in 1872, opened here in Ueno Park in 1882.",
             note_it="Il più antico museo nazionale del Giappone, fondato nel 1872, aprì qui nel parco di Ueno nel 1882."),
        # Source: Wikipedia, Tokyo National Museum; Wikipedia (ja), 東京国立博物館 (Japan's oldest museum). Which is its largest differs (art museum, or museum): not said.
        Stop("tokyo_toshogu", "Ueno Tōshō-gū", "Ueno Tōshō-gū", 35.7154, 139.7706,
             note_en="A shrine to Tokugawa Ieyasu, the first Tokugawa shōgun; its buildings, renewed by Iemitsu in 1651, have come down almost intact.",
             note_it="È un santuario dedicato a Tokugawa Ieyasu, il primo shōgun Tokugawa; i suoi edifici, rinnovati da Iemitsu nel 1651, sono giunti quasi intatti."),
        # Source: Wikipedia, Ueno Tōshō-gū; Wikipedia (ja), 上野東照宮.
        Stop("tokyo_shinobazu", "Shinobazu Pond", "Stagno Shinobazu", 35.7122, 139.7708,
             note_en="In summer lotus leaves cover part of the pond; on its island stands a temple to the goddess Benzaiten.",
             note_it="D'estate le foglie di loto coprono parte dello stagno; sulla sua isola c'è un tempio dedicato alla dea Benzaiten."),
        # Source: Wikipedia, Shinobazu Pond; Wikipedia (ja), 不忍池.
        Stop("tokyo_ameyoko", "Ameyoko", "Ameyoko", 35.7095, 139.7745,
             note_en="This market along the railway may take its name from the sweets sold here after the war, or from American army goods.",
             note_it="Questo mercato lungo la ferrovia prenderebbe il nome dai dolci venduti qui nel dopoguerra, o dalle merci dell'esercito americano."),
        # Source: Wikipedia, Ameya-Yokochō; Wikipedia (ja), アメ横. Both give the two theories.
        Stop("tokyo_kanda_myojin", "Kanda Shrine", "Santuario di Kanda", 35.7019, 139.7677,
             note_en="Founded, by tradition, in 730, the shrine was moved as Edo Castle grew, and came to this hill in 1616.",
             note_it="Fondato, secondo la tradizione, nel 730, il santuario fu spostato via via che il castello di Edo cresceva, e giunse su questa collina nel 1616."),
        # Source: Wikipedia, Kanda Shrine; Wikipedia (ja), 神田明神.
        Stop("tokyo_yushima_seido", "Yushima Seidō", "Yushima Seidō", 35.7004, 139.7666,
             note_en="The shōgun Tsunayoshi set this Confucian temple here; in 1872 it held the exhibition from which the national museum was born.",
             note_it="Lo shōgun Tsunayoshi volle qui questo tempio confuciano; nel 1872 ospitò la mostra da cui nacque il museo nazionale."),
        # Source: Wikipedia, Yushima Seidō; Wikipedia (ja), 湯島聖堂; for the exhibition, Wikipedia, Tokyo National Museum, and (ja) 東京国立博物館. The year it came here differs (1690 or 1691): not said.
        Stop("tokyo_akihabara", "Akihabara", "Akihabara", 35.6985, 139.7712,
             note_en="After the war Akihabara grew from a black market into Electric Town, and later the heart of otaku culture.",
             note_it="Nel dopoguerra Akihabara passò dal mercato nero alla Città elettrica, e divenne poi il cuore della cultura otaku."),
        # Source: Wikipedia, Akihabara; Wikipedia (ja), 秋葉原.
        Stop("tokyo_mitsukoshi", "Mitsukoshi", "Mitsukoshi", 35.6862, 139.7735,
             note_en="Mitsukoshi began in 1673 as Echigoya, a kimono shop; this store was finished in 1914.",
             note_it="Mitsukoshi nacque nel 1673 come Echigoya, un negozio di kimono; questo grande magazzino fu finito nel 1914."),
        # Source: Wikipedia, Mitsukoshi; Wikipedia (ja), 三越日本橋本店. Whether it was Japan's first department store is debated: not said.
        Stop("tokyo_nihonbashi", "Nihonbashi", "Nihonbashi", 35.684, 139.774,
             note_en="The five great roads of the Edo period began at this bridge, and road distances to Tokyo are still counted from here.",
             note_it="Le cinque grandi strade dell'epoca Edo partivano da questo ponte, e le distanze stradali per Tokyo si contano ancora da qui."),
        # Source: Wikipedia, Nihonbashi; Wikipedia (ja), 日本橋 (東京都中央区).
        Stop("tokyo_station", "Tokyo Station", "Stazione di Tokyo", 35.6812, 139.766,
             note_en="Tatsuno Kingo's red-brick station opened in 1914; damaged in the bombing of 1945, it was restored as it first stood in 2012.",
             note_it="La stazione in mattoni rossi di Tatsuno Kingo aprì nel 1914; danneggiata dai bombardamenti del 1945, nel 2012 tornò com'era in origine."),
        # Source: Wikipedia, Tokyo Station; Wikipedia (ja), 東京駅.
        Stop("tokyo_imperial_palace", "Imperial Palace", "Palazzo imperiale", 35.6797, 139.755,
             note_en="Only at the New Year and on the Emperor's birthday may the public cross into the palace, where the imperial family greets them.",
             note_it="Solo a Capodanno e per il compleanno dell'imperatore il pubblico può entrare nel palazzo, dove la famiglia imperiale lo saluta."),
        # Source: Wikipedia, Tokyo Imperial Palace; Wikipedia (ja), 二重橋 (the gate opened, and the bridge crossed, on those days).
    ],
)

SYDNEY = Walk(
    id="SYDNEY_LUNA_PARK_GARDEN",
    city="sydney",
    city_en="Sydney",
    city_it="Sydney",
    route_en="From Luna Park to the Chinese Garden, over the Harbour Bridge and by the Opera House",
    route_it="Dal Luna Park al Giardino cinese, attraverso l'Harbour Bridge e passando per l'Opera House",
    outing_en="A walk in Sydney",
    outing_it="Passeggiata a Sydney",
    country="AU",
    continent="ASIA_OCEANIA",
    # The harbour is a water area in OpenStreetMap, its coastline out at the Heads.
    water=["relation/1252425"],
    # The Botanic Garden and the Domain, Hyde Park, the parks at the bridge's ends, Darling Harbour's.
    parks=[
        "relation/3744999", "relation/3744998", "way/1224921215", "relation/2030042", "way/4334301",
        "relation/2068542", "way/55218510", "way/183246960", "way/4334305",
    ],
    stops=[
        Stop("sydney_luna_park", "Luna Park", "Luna Park", -33.8479, 151.21,
             note_en="Built in 1935, the park is entered through a giant face, made again several times since.",
             note_it="Costruito nel 1935, al parco si entra attraverso un volto gigante, rifatto più volte da allora."),
        # Source: Wikipedia, Luna Park Sydney; Wikipedia (de), Luna Park (Sydney).
        Stop("sydney_harbour_bridge", "Sydney Harbour Bridge", "Sydney Harbour Bridge", -33.8525, 151.2108,
             note_en="Sydney calls this steel arch, opened in 1932, the Coathanger.",
             note_it="Sydney chiama Coathanger, l'attaccapanni, questo arco d'acciaio inaugurato nel 1932."),
        # Source: Wikipedia, Sydney Harbour Bridge; Wikipedia (de), Sydney Harbour Bridge. Its records are given differently (the tallest steel arch and once the widest, or the widest): not said.
        Stop("sydney_cadmans_cottage", "Cadmans Cottage", "Cadmans Cottage", -33.8589, 151.2092,
             note_en="Built in 1816 for the government's boat crews, the cottage stood by the water, now about 100 m away.",
             note_it="Costruita nel 1816 per gli equipaggi delle barche del governo, la casetta era in riva all'acqua, che oggi è a circa 100 m."),
        # Source: Wikipedia, Cadmans Cottage; Wikipedia (de), Cadmans Cottage. Which is older differs (the second oldest house in Sydney, or the oldest building of The Rocks): not said.
        Stop("sydney_circular_quay", "Circular Quay", "Circular Quay", -33.8612, 151.211,
             note_en="The First Fleet landed in this cove in 1788, and founded the settlement that became Sydney.",
             note_it="La Prima Flotta approdò in questa baia nel 1788, e fondò l'insediamento da cui nacque Sydney."),
        # Source: Wikipedia, Sydney Cove; Wikipedia (de), Circular Quay.
        Stop("sydney_opera_house", "Sydney Opera House", "Opera House di Sydney", -33.858, 151.2148,
             note_en="Designed by the Dane Jørn Utzon, it was opened by Queen Elizabeth II in 1973, and has been a World Heritage Site since 2007.",
             note_it="Progettata dal danese Jørn Utzon, fu inaugurata dalla regina Elisabetta II nel 1973 ed è patrimonio dell'umanità dal 2007."),
        # Source: Wikipedia, Sydney Opera House; Wikipedia (it), Teatro dell'Opera di Sydney.
        Stop("sydney_botanic_garden", "Royal Botanic Garden", "Royal Botanic Garden", -33.864, 151.217,
             note_en="Founded in 1816 on Farm Cove, where the colony's first farm was laid out, it is Australia's oldest scientific institution.",
             note_it="Fondato nel 1816 sulla Farm Cove, dove sorse la prima fattoria della colonia, è la più antica istituzione scientifica dell'Australia."),
        # Source: Wikipedia, Royal Botanic Garden, Sydney; Wikipedia (de), Royal Botanic Gardens (Sydney).
        Stop("sydney_macquarie_chair", "Mrs Macquarie's Chair", "Mrs Macquarie's Chair", -33.8597, 151.2224,
             note_en="Convicts carved this seat in the sandstone in 1810 for Elizabeth Macquarie, the governor's wife.",
             note_it="Nel 1810 alcuni detenuti scavarono questo sedile nell'arenaria per Elizabeth Macquarie, la moglie del governatore."),
        # Source: Wikipedia, Mrs Macquarie's Chair; Wikipedia (de), Mrs Macquarie’s Chair.
        Stop("sydney_art_gallery", "Art Gallery of New South Wales", "Art Gallery of New South Wales", -33.8684, 151.2172,
             note_en="In 2022 the gallery opened a second building, designed by the Japanese studio SANAA.",
             note_it="Nel 2022 il museo ha aperto un secondo edificio, progettato dallo studio giapponese SANAA."),
        # Source: Wikipedia, Art Gallery of New South Wales; Wikipedia (de), Art Gallery of New South Wales.
        Stop("sydney_hyde_park_barracks", "Hyde Park Barracks", "Hyde Park Barracks", -33.8697, 151.2124,
             note_en="Francis Greenway designed these barracks for the colony's male convicts; they are a World Heritage Site.",
             note_it="Francis Greenway progettò queste caserme per i detenuti maschi della colonia; sono patrimonio dell'umanità."),
        # Source: Wikipedia, Hyde Park Barracks, Sydney; Wikipedia (fr), Hyde Park Barracks (Sydney). The years they were built differ (from 1817 or 1818): not said.
        Stop("sydney_st_marys", "St Mary's Cathedral", "Cattedrale di Santa Maria", -33.8714, 151.2128,
             note_en="Begun after a fire destroyed the first church in 1865, William Wardell's cathedral was largely finished in 1928.",
             note_it="Iniziata dopo che un incendio distrusse la prima chiesa nel 1865, la cattedrale di William Wardell fu quasi finita nel 1928."),
        # Source: Wikipedia, St Mary's Cathedral, Sydney; Wikipedia (it), Cattedrale di Santa Maria (Sydney).
        Stop("sydney_anzac_memorial", "Anzac Memorial", "Memoriale degli Anzac", -33.8752, 151.2108,
             note_en="This Art Deco memorial in Hyde Park, designed by Bruce Dellit, opened in 1934.",
             note_it="Questo memoriale Art déco a Hyde Park, progettato da Bruce Dellit, fu inaugurato nel 1934."),
        # Source: Wikipedia, Anzac Memorial; Wikipedia (it), Memoriale degli Anzac.
        Stop("sydney_qvb", "Queen Victoria Building", "Queen Victoria Building", -33.8717, 151.2069,
             note_en="George McRae's market hall, finished in 1898, was nearly pulled down in 1959 and now holds shops again.",
             note_it="Il mercato coperto di George McRae, finito nel 1898, rischiò la demolizione nel 1959 e oggi ospita di nuovo negozi."),
        # Source: Wikipedia, Queen Victoria Building; Wikipedia (de), Queen Victoria Building.
        Stop("sydney_darling_harbour", "Darling Harbour", "Darling Harbour", -33.8707, 151.2015,
             note_en="Once a port of wharves and railway yards, Darling Harbour is now mostly for people on foot.",
             note_it="Un tempo porto di moli e scali ferroviari, Darling Harbour è oggi soprattutto per chi va a piedi."),
        # Source: Wikipedia, Darling Harbour; Wikipedia (de), Darling Harbour. Pyrmont Bridge, crossed here, has an article in English only.
        Stop("sydney_chinese_garden", "Chinese Garden of Friendship", "Giardino cinese dell'Amicizia", -33.8763, 151.2028,
             note_en="Designed by Guangzhou, Sydney's Chinese sister city, the garden opened in 1988, for the bicentenary.",
             note_it="Progettato da Canton, la città cinese gemellata con Sydney, il giardino fu aperto nel 1988, per il bicentenario."),
        # Source: Wikipedia, Chinese Garden of Friendship; Wikipedia (es), Jardín chino de la Amistad.
    ],
)

SEOUL = Walk(
    id="SEOUL_GWANGHWAMUN_NAMSAN",
    city="seoul",
    city_en="Seoul",
    city_it="Seul",
    route_en="From Gwanghwamun Square to N Seoul Tower, by the palaces and the Cheonggyecheon",
    route_it="Da piazza Gwanghwamun alla N Seoul Tower, passando per i palazzi e il Cheonggyecheon",
    outing_en="A walk in Seoul",
    outing_it="Passeggiata a Seul",
    country="KR",
    continent="ASIA_OCEANIA",
    # The palaces' ponds. The Cheonggyecheon is a narrow stream: drawn as a line, as wide as it is.
    water=["relation/4005465", "relation/13407146", "relation/15457595", "relation/5672591"],
    canals=["way/368276771", "way/769631455"],
    # Namsan's woods, Jongmyo's, Changdeokgung's Secret Garden, Gyeongbokgung's lawns, the hills behind.
    parks=[
        "way/244397333", "relation/5688761", "relation/10799909", "relation/10966163", "relation/6638151",
        "relation/10351443", "way/624285855", "relation/10804175", "way/370019676",
    ],
    stops=[
        Stop("seoul_gwanghwamun_square", "Gwanghwamun Square", "Piazza Gwanghwamun", 37.5711, 126.9769,
             note_en="Opened in 2009 in front of Gyeongbokgung, the square holds the statues of King Sejong and Admiral Yi Sun-sin.",
             note_it="Aperta nel 2009 davanti al Gyeongbokgung, la piazza ospita le statue del re Sejong e dell'ammiraglio Yi Sun-sin."),
        # Source: Wikipedia, Gwanghwamun Square; Wikipedia (ko), 광화문광장.
        Stop("seoul_gwanghwamun", "Gwanghwamun", "Gwanghwamun", 37.5753, 126.9769,
             note_en="Gyeongbokgung's great gate was moved under Japanese rule, burned in the Korean War, and stood again in its place in 2010.",
             note_it="La grande porta del Gyeongbokgung fu spostata sotto il dominio giapponese, bruciò nella guerra di Corea e nel 2010 tornò al suo posto."),
        # Source: Wikipedia, Gwanghwamun; Wikipedia (ko), 광화문.
        Stop("seoul_bukchon", "Bukchon Hanok Village", "Bukchon Hanok Village", 37.5826, 126.985,
             note_en="Bukchon means north village, north of the Cheonggyecheon; its lanes keep many hanok, traditional Korean houses.",
             note_it="Bukchon vuol dire villaggio del nord, a nord del Cheonggyecheon; i suoi vicoli conservano molti hanok, le case tradizionali coreane."),
        # Source: Wikipedia, Bukchon Hanok Village; Wikipedia (ko), 북촌 한옥마을.
        Stop("seoul_changdeokgung", "Changdeokgung", "Changdeokgung", 37.5794, 126.991,
             note_en="Built in 1405, after Gyeongbokgung, this palace was for centuries the kings' main seat, and is a World Heritage Site.",
             note_it="Costruito nel 1405, dopo il Gyeongbokgung, questo palazzo fu per secoli la sede principale dei re ed è patrimonio dell'umanità."),
        # Source: Wikipedia, Changdeokgung; Wikipedia (ko), 창덕궁.
        Stop("seoul_jongmyo", "Jongmyo", "Jongmyo", 37.5716, 126.9941,
             note_en="The spirit tablets of Joseon's kings and queens are kept in this shrine, a World Heritage Site since 1995.",
             note_it="In questo santuario sono custodite le tavolette spirituali dei re e delle regine di Joseon; è patrimonio dell'umanità dal 1995."),
        # Source: Wikipedia, Jongmyo; Wikipedia (ko), 종묘.
        Stop("seoul_insadong", "Insa-dong", "Insa-dong", 37.574, 126.9852,
             note_en="Insa-dong's antique trade began under Japanese rule; galleries came later, and fill its alleys today.",
             note_it="Il commercio di antichità di Insa-dong cominciò sotto il dominio giapponese; poi arrivarono le gallerie, che oggi riempiono i suoi vicoli."),
        # Source: Wikipedia, Insa-dong; Wikipedia (ko), 인사동.
        Stop("seoul_jogyesa", "Jogyesa", "Jogyesa", 37.5737, 126.9818,
             note_en="Founded in 1910 and named Jogyesa in 1954, the temple keeps in its courtyard a white pine protected as a natural monument.",
             note_it="Fondato nel 1910 e chiamato Jogyesa dal 1954, il tempio custodisce nel cortile un pino bianco protetto come monumento naturale."),
        # Source: Wikipedia, Jogyesa; Wikipedia (ko), 조계사.
        Stop("seoul_cheonggyecheon", "Cheonggyecheon", "Cheonggyecheon", 37.569, 126.9813,
             note_en="Covered last century by concrete and an elevated road, the stream was brought back to light in 2005.",
             note_it="Coperto nel secolo scorso dal cemento e da una strada sopraelevata, il torrente tornò alla luce nel 2005."),
        # Source: Wikipedia, Cheonggyecheon; Wikipedia (ko), 청계천.
        Stop("seoul_deoksugung", "Deoksugung", "Deoksugung", 37.5658, 126.9768,
             note_en="The main palace of the Korean Empire, proclaimed by Gojong in 1897, it mixes Korean halls with Western buildings.",
             note_it="Palazzo principale dell'Impero coreano, proclamato da Gojong nel 1897, unisce padiglioni coreani ed edifici occidentali."),
        # Source: Wikipedia, Deoksugung; Wikipedia (ko), 덕수궁.
        Stop("seoul_sungnyemun", "Sungnyemun", "Sungnyemun", 37.5603, 126.9754,
             note_en="The old south gate of Seoul's walls burned in an arson attack in 2008, and was restored by 2013.",
             note_it="L'antica porta sud delle mura di Seul bruciò in un incendio doloso nel 2008 e fu restaurata entro il 2013."),
        # Source: Wikipedia, Namdaemun; Wikipedia (ko), 숭례문. The year it was built differs (1396 or 1398): not said.
        Stop("seoul_namdaemun_market", "Namdaemun Market", "Mercato di Namdaemun", 37.5592, 126.9776,
             note_en="In 1414 the court built shops here by the south gate: the beginning of today's market.",
             note_it="Nel 1414 la corte costruì qui botteghe presso la porta sud: è l'inizio del mercato di oggi."),
        # Source: Wikipedia, Namdaemun Market; Wikipedia (ko), 남대문시장.
        Stop("seoul_n_tower", "N Seoul Tower", "N Seoul Tower", 37.5513, 126.9882,
             note_en="Finished in 1975 on Namsan, the 236-metre tower sends out the broadcasters' signals over Seoul.",
             note_it="Finita nel 1975 sul Namsan, la torre alta 236 metri trasmette i segnali delle emittenti su Seul."),
        # Source: Wikipedia, Namsan Seoul Tower; Wikipedia (ko), YTN서울타워.
    ],
)

BEIJING = Walk(
    id="BEIJING_TIANANMEN_YONGHE",
    city="beijing",
    city_en="Beijing",
    city_it="Pechino",
    route_en="From Tiananmen Square to the Lama Temple, through the Forbidden City and by Beihai and the Drum Tower",
    route_it="Da piazza Tienanmen al Tempio dei Lama, attraverso la Città Proibita e passando per Beihai e la Torre del Tamburo",
    outing_en="A walk in Beijing",
    outing_it="Passeggiata a Pechino",
    country="CN",
    continent="ASIA_OCEANIA",
    # The Forbidden City's moat; Beihai, Zhonghai and Nanhai; Shichahai's lakes.
    water=[
        "way/4845030", "relation/5451458", "relation/68127", "relation/3531212", "relation/11518929",
        "relation/409777",
    ],
    # Jingshan, Beihai, Zhongshan Park, the parks along the Imperial City's old wall, Prince Gong's garden.
    parks=[
        "way/29201967", "way/366464114", "relation/18320943", "relation/9054321", "way/30843688",
        "relation/9509823", "way/268548508",
    ],
    stops=[
        Stop("beijing_tiananmen_square", "Tiananmen Square", "Piazza Tienanmen", 39.9032, 116.3918,
             note_en="The square lies where the Ming and the Qing had their Corridor of a Thousand Steps; it was widened in the 1950s.",
             note_it="La piazza sorge dove i Ming e i Qing avevano il Corridoio dei mille passi; fu allargata negli anni Cinquanta."),
        # Source: Wikipedia, Tiananmen Square; Wikipedia (zh), 天安门广场.
        Stop("beijing_tiananmen", "Tiananmen", "Tienanmen", 39.9073, 116.3913,
             note_en="Called Chengtianmen under the Ming, this gate of the Imperial City now appears on China's national emblem.",
             note_it="Sotto i Ming si chiamava Chengtianmen; questa porta della Città imperiale compare oggi sull'emblema nazionale cinese."),
        # Source: Wikipedia, Tiananmen; Wikipedia (zh), 天安门. The year it was built differs (1417 or 1420): not said.
        # Through the Forbidden City (owner, 6 Oct 2026): in by the Meridian Gate, out by the Gate of
        # Divine Might, as visitors go; the places inside are closer than elsewhere, by design.
        Stop("beijing_meridian_gate", "Meridian Gate", "Porta Meridiana", 39.9128, 116.3912,
             note_en="Through the Meridian Gate, its south gate, you enter the Forbidden City, home of the emperors from 1420 until 1924.",
             note_it="Dalla Porta Meridiana, la sua porta sud, si entra nella Città Proibita, dimora degli imperatori dal 1420 al 1924."),
        # Source: Wikipedia, Meridian Gate, and Forbidden City (the emperors' residence from 1420 to 1924); Wikipedia (zh), 午門 (北京), and 故宫 (finished in 1420, Puyi gone in 1924).
        Stop("beijing_supreme_harmony", "Hall of Supreme Harmony", "Sala della Suprema Armonia", 39.9155, 116.3908,
             note_en="The palace's largest hall, on a terrace of three marble tiers, is where the Ming and Qing emperors were enthroned.",
             note_it="La sala più grande del palazzo, su una terrazza di marmo a tre livelli, è dove salivano al trono gli imperatori Ming e Qing."),
        # Source: Wikipedia, Hall of Supreme Harmony; Wikipedia (zh), 太和殿.
        Stop("beijing_heavenly_purity", "Palace of Heavenly Purity", "Palazzo della Purezza Celeste", 39.9183, 116.3908,
             note_en="From the Yongzheng Emperor on, the Qing hid the name of their heir behind the tablet above this throne.",
             note_it="Dall'imperatore Yongzheng in poi, i Qing nascosero il nome del loro erede dietro la tavoletta sopra questo trono."),
        # Source: Wikipedia, Palace of Heavenly Purity; Wikipedia (zh), 乾清宫.
        Stop("beijing_divine_might", "Gate of Divine Might", "Porta del Vigore Divino", 39.9218, 116.3906,
             note_en="Past the Imperial Garden, you leave the Forbidden City by its north gate, first named for the Black Tortoise of the north.",
             note_it="Passato il Giardino Imperiale, si esce dalla Città Proibita dalla sua porta nord, che in origine prendeva il nome dalla Tartaruga Nera del nord."),
        # Source: Wikipedia, Gate of Divine Prowess, and Forbidden City (the garden south of the gate); Wikipedia (zh), 神武門. The Imperial Garden has an article in Chinese only: named, not a stop.
        Stop("beijing_jingshan", "Jingshan Park", "Parco Jingshan", 39.9236, 116.3917,
             note_en="In 1644, as rebels took Beijing, the last Ming emperor, Chongzhen, hanged himself from a tree on this hill.",
             note_it="Nel 1644, mentre i ribelli prendevano Pechino, l'ultimo imperatore Ming, Chongzhen, si impiccò a un albero su questa collina."),
        # Source: Wikipedia, Jingshan Park; Wikipedia (zh), 景山公园. When the hill was raised differs: not said.
        Stop("beijing_white_dagoba", "White Dagoba", "Dagoba Bianco", 39.9255, 116.3836,
             note_en="Raised in 1651, the White Dagoba crowns Jade Flower Island, in the middle of Beihai's lake.",
             note_it="Eretto nel 1651, il Dagoba Bianco corona l'isola dei Fiori di Giada, in mezzo al lago di Beihai."),
        # Source: Wikipedia, Beihai Park; Wikipedia (zh), 北海公园. Why it was built differs (a Dalai Lama's visit, a lama's request), and the year the park opened (1922 or 1925): not said.
        Stop("beijing_nine_dragon_wall", "Nine-Dragon Wall", "Muro dei Nove Draghi", 39.931, 116.3822,
             note_en="Built in 1756, this screen wall has nine dragons on each side.",
             note_it="Costruito nel 1756, questo muro schermo ha nove draghi su ciascun lato."),
        # Source: Wikipedia, Nine-Dragon Wall; Wikipedia (zh), 九龙壁.
        Stop("beijing_prince_gong", "Prince Gong's Mansion", "Residenza del principe Gong", 39.9362, 116.3815,
             note_en="Built for Heshen, a minister of the Qianlong Emperor, the mansion later became the home of Prince Gong, whose name it keeps.",
             note_it="Costruita per Heshen, ministro dell'imperatore Qianlong, la residenza fu poi la casa del principe Gong, di cui porta il nome."),
        # Source: Wikipedia, Prince Gong's Mansion; Wikipedia (zh), 恭王府. The years differ (1777, or 1780 to 1788): not said.
        Stop("beijing_shichahai", "Shichahai", "Shichahai", 39.9376, 116.3872,
             note_en="Shichahai's three lakes were once part of the Grand Canal, which reached Beijing from Hangzhou.",
             note_it="I tre laghi di Shichahai facevano parte del Canale Imperiale, che arrivava a Pechino da Hangzhou."),
        # Source: Wikipedia, Shichahai; Wikipedia (zh), 什刹海. The stop is the Yinding Bridge.
        Stop("beijing_drum_tower", "Drum Tower", "Torre del Tamburo", 39.939, 116.3897,
             note_en="First built in 1272, under the Yuan, the Drum Tower was rebuilt here under the Ming.",
             note_it="Costruita per la prima volta nel 1272, sotto gli Yuan, la Torre del Tamburo fu ricostruita qui sotto i Ming."),
        # Source: Wikipedia, Drum Tower and Bell Tower of Beijing; Wikipedia (zh), 北京鼓楼和钟楼.
        Stop("beijing_bell_tower", "Bell Tower", "Torre della Campana", 39.9412, 116.3897,
             note_en="Just behind the Drum Tower, the Bell Tower kept the city's official time with it until 1924.",
             note_it="Subito dietro la Torre del Tamburo, la Torre della Campana scandì con lei l'ora ufficiale della città fino al 1924."),
        # Source: Wikipedia, Drum Tower and Bell Tower of Beijing; Wikipedia (zh), 北京鼓楼和钟楼.
        Stop("beijing_nanluoguxiang", "Nanluoguxiang", "Nanluoguxiang", 39.9403, 116.3966,
             note_en="Nearly 800 metres long, this hutong took its present name under the Qing, by about 1750.",
             note_it="Lungo quasi 800 metri, questo hutong prese il nome attuale sotto i Qing, intorno al 1750."),
        # Source: Wikipedia, Nanluoguxiang; Wikipedia (zh), 南锣鼓巷. The stop is its north end, crossed on the way east.
        Stop("beijing_lama_temple", "Lama Temple", "Tempio dei Lama", 39.9435, 116.4116,
             note_en="Built as the home of Prince Yong, the future Yongzheng Emperor, it became a monastery of Tibetan Buddhism's Gelug school in 1744.",
             note_it="Nata come residenza del principe Yong, il futuro imperatore Yongzheng, nel 1744 divenne un monastero della scuola Gelug del buddhismo tibetano."),
        # Source: Wikipedia, Yonghe Temple; Wikipedia (zh), 雍和宫.
    ],
)

HONG_KONG = Walk(
    id="HONG_KONG_VICTORIA_WESTERN",
    city="hong_kong",
    city_en="Hong Kong",
    city_it="Hong Kong",
    route_en="From Victoria Park to Western Market, by Wan Chai, the harbour and Central",
    route_it="Da Victoria Park al Western Market, passando per Wan Chai, il porto e Central",
    outing_en="A walk in Hong Kong",
    outing_it="Passeggiata a Hong Kong",
    country="HK",
    continent="ASIA_OCEANIA",
    # The harbour, from the coastline.
    coast=True,
    water=[],
    # Victoria Park, Hong Kong Park, the Botanical Gardens, Tamar Park, Chater Garden, the woods above.
    parks=[
        "way/4182605", "way/42255430", "relation/12955557", "way/138602057", "way/148784038",
        "relation/11794569", "relation/4186983", "relation/11795322",
    ],
    stops=[
        Stop("hong_kong_victoria_park", "Victoria Park", "Victoria Park", 22.2815, 114.1885,
             note_en="Opened in 1957 on land reclaimed from the old Causeway Bay typhoon shelter, the park is named after Queen Victoria.",
             note_it="Aperto nel 1957 su terreni strappati al vecchio rifugio per tifoni di Causeway Bay, il parco porta il nome della regina Vittoria."),
        # Source: Wikipedia, Victoria Park (Hong Kong); Wikipedia (zh), 維多利亞公園.
        Stop("hong_kong_noonday_gun", "Noonday Gun", "Noonday Gun", 22.283, 114.1832,
             note_en="Every day at noon Jardines fires this gun, in amends, the story goes, for a salute once fired for the firm's own head.",
             note_it="Ogni giorno a mezzogiorno la Jardines spara questo cannone, per rimediare, si racconta, a una salva sparata un tempo per il capo della ditta."),
        # Source: Wikipedia, Noonday Gun (a penalty); Wikipedia (zh), 怡和午炮 (an apology): the sentence says amends.
        Stop("hong_kong_pak_tai", "Pak Tai Temple", "Tempio di Pak Tai", 22.2731, 114.1738,
             note_en="Built by the people of Wan Chai in 1863, the temple keeps a statue of Pak Tai three metres tall.",
             note_it="Costruito dagli abitanti di Wan Chai nel 1863, il tempio custodisce una statua di Pak Tai alta tre metri."),
        # Source: Wikipedia, Wan Chai Pak Tai Temple; Wikipedia (zh), 灣仔北帝廟.
        Stop("hong_kong_golden_bauhinia", "Golden Bauhinia Square", "Piazza del Bauhinia d'oro", 22.284, 114.1738,
             note_en="A gilded bauhinia six metres tall marks the handover of Hong Kong in 1997; the flag is raised here every morning at eight.",
             note_it="Un bauhinia dorato alto sei metri ricorda il passaggio di Hong Kong del 1997; qui la bandiera sale ogni mattina alle otto."),
        # Source: Wikipedia, Golden Bauhinia Square; Wikipedia (zh), 金紫荊廣場.
        Stop("hong_kong_star_ferry", "Star Ferry Pier", "Molo dello Star Ferry", 22.287, 114.161,
             note_en="From here the Star Ferry crosses the harbour to Tsim Sha Tsui, a service begun by Dorabjee Naorojee Mithaiwala, a Parsi.",
             note_it="Da qui lo Star Ferry attraversa il porto fino a Tsim Sha Tsui, un servizio avviato da Dorabjee Naorojee Mithaiwala, un parsi."),
        # Source: Wikipedia, Star Ferry; Wikipedia (zh), 天星小輪. The year it began differs (1880 or 1888): not said.
        Stop("hong_kong_statue_square", "Statue Square", "Statue Square", 22.2812, 114.1603,
             note_en="Named for Queen Victoria's statue, now in Victoria Park, the square keeps a single statue, of the banker Sir Thomas Jackson.",
             note_it="Il nome viene dalla statua della regina Vittoria, oggi a Victoria Park; nella piazza resta una sola statua, del banchiere Sir Thomas Jackson."),
        # Source: Wikipedia, Statue Square; Wikipedia (zh), 皇后像廣場.
        Stop("hong_kong_hsbc", "HSBC Building", "Sede della HSBC", 22.28, 114.1593,
             note_en="Opened in 1986, the bank's tower shows its steel frame outside; the bronze lions at its door follow those of its Shanghai office.",
             note_it="Aperta nel 1986, la sede della banca mostra all'esterno la sua struttura d'acciaio; i leoni di bronzo all'ingresso imitano quelli della sede di Shanghai."),
        # Source: Wikipedia, HSBC Building (Hong Kong); Wikipedia (zh), 滙豐總行大廈.
        Stop("hong_kong_bank_of_china", "Bank of China Tower", "Bank of China Tower", 22.2795, 114.1612,
             note_en="Designed by I. M. Pei, it was the tallest building in Hong Kong and in Asia when it opened in 1990.",
             note_it="Progettata da I. M. Pei, quando aprì nel 1990 era l'edificio più alto di Hong Kong e dell'Asia."),
        # Source: Wikipedia, Bank of China Tower (Hong Kong); Wikipedia (zh), 中銀大廈 (香港).
        Stop("hong_kong_flagstaff_house", "Flagstaff House", "Flagstaff House", 22.278, 114.1625,
             note_en="Built in 1846 for the commander of the British forces, the house is now a museum of tea ware.",
             note_it="Costruita nel 1846 per il comandante delle forze britanniche, la casa è oggi un museo di teiere e servizi da tè."),
        # Source: Wikipedia, Flagstaff House, Hong Kong; Wikipedia (zh), 茶具文物館.
        Stop("hong_kong_st_johns", "St John's Cathedral", "Cattedrale di San Giovanni", 22.2789, 114.1599,
             note_en="Hong Kong's oldest Western church, finished in 1849, stands on the only freehold land in the city.",
             note_it="La più antica chiesa occidentale di Hong Kong, finita nel 1849, sorge sull'unico terreno in piena proprietà della città."),
        # Source: Wikipedia, St John's Cathedral (Hong Kong); Wikipedia (zh), 聖約翰座堂 (香港).
        Stop("hong_kong_peak_tram", "Peak Tram", "Peak Tram", 22.2776, 114.1597,
             note_en="Opened in 1888, the funicular climbs from here to Victoria Peak.",
             note_it="Aperta nel 1888, la funicolare sale da qui fino al Victoria Peak."),
        # Source: Wikipedia, Peak Tram; Wikipedia (zh), 山頂纜車.
        Stop("hong_kong_tai_kwun", "Tai Kwun", "Tai Kwun", 22.2814, 114.1546,
             note_en="The old Central Police Station, with the magistracy and Victoria Prison, reopened to the public in 2018 as Tai Kwun.",
             note_it="La vecchia stazione di polizia centrale, con il tribunale e la prigione Victoria, ha riaperto al pubblico nel 2018 come Tai Kwun."),
        # Source: Wikipedia, Tai Kwun; Wikipedia (zh), 大館.
        Stop("hong_kong_pmq", "PMQ", "PMQ", 22.2832, 114.152,
             note_en="Once quarters for married policemen, on the site of Queen's College, the buildings became a creative centre in 2014.",
             note_it="Un tempo alloggi per poliziotti sposati, dove sorgeva il Queen's College, gli edifici sono diventati un centro creativo nel 2014."),
        # Source: Wikipedia, PMQ (Hong Kong); Wikipedia (zh), 元創方 (the Central School, Queen's College's first name).
        Stop("hong_kong_man_mo", "Man Mo Temple", "Tempio di Man Mo", 22.284, 114.1503,
             note_en="The temple honours Man Cheong, god of literature, and Kwan Tai, god of war; the Tung Wah hospitals have run it since 1908.",
             note_it="Il tempio onora Man Cheong, dio della letteratura, e Kwan Tai, dio della guerra; dal 1908 è gestito dagli ospedali Tung Wah."),
        # Source: Wikipedia, Man Mo temples in Hong Kong; Wikipedia (zh), 東華三院文武廟. Its year differs (1847, or 1847 to 1862): not said.
        Stop("hong_kong_western_market", "Western Market", "Western Market", 22.2873, 114.1502,
             note_en="Built in 1906, this was the north block of the old Western Market.",
             note_it="Costruito nel 1906, era il blocco nord del vecchio Western Market."),
        # Source: Wikipedia, Western Market; Wikipedia (zh), 西港城.
    ],
)

SINGAPORE = Walk(
    id="SINGAPORE_CHINATOWN_GARDENS",
    city="singapore",
    city_en="Singapore",
    city_it="Singapore",
    route_en="From Chinatown to Gardens by the Bay, by Fort Canning, the river and Marina Bay",
    route_it="Da Chinatown ai Gardens by the Bay, passando per Fort Canning, il fiume e Marina Bay",
    outing_en="A walk in Singapore",
    outing_it="Passeggiata a Singapore",
    country="SG",
    continent="ASIA_OCEANIA",
    # Inside the Marina Barrage, Marina Bay is a reservoir, with the river; the sea's coastline is
    # beyond the barrage, outside the map.
    water=["relation/9542146", "relation/9569901"],
    parks=["way/16892550", "relation/10231144", "relation/9976848", "way/687917300", "way/460428718"],
    stops=[
        Stop("singapore_buddha_tooth", "Buddha Tooth Relic Temple", "Tempio della Reliquia del Dente di Buddha", 1.2815, 103.8443,
             note_en="In the heart of Chinatown, this temple keeps a relic held to be a tooth of the Buddha.",
             note_it="Nel cuore di Chinatown, questo tempio custodisce una reliquia ritenuta un dente del Buddha."),
        # Source: Wikipedia, Buddha Tooth Relic Temple and Museum ("it is claimed"); Wikipedia (zh), 佛牙寺龙华院.
        Stop("singapore_sri_mariamman", "Sri Mariamman Temple", "Tempio di Sri Mariamman", 1.2827, 103.8455,
             note_en="Founded in 1827 by Naraina Pillai, this is Singapore's oldest Hindu temple.",
             note_it="Fondato nel 1827 da Naraina Pillai, è il più antico tempio indù di Singapore."),
        # Source: Wikipedia, Sri Mariamman Temple, Singapore; Wikipedia (zh), 马里安曼庙.
        Stop("singapore_clarke_quay", "Clarke Quay", "Clarke Quay", 1.289, 103.8463,
             note_en="Named after the governor Sir Andrew Clarke, the quay's old warehouses are now restaurants and bars.",
             note_it="Intitolato al governatore Sir Andrew Clarke, il molo ha i vecchi magazzini trasformati in ristoranti e locali."),
        # Source: Wikipedia, Clarke Quay; Wikipedia (zh), 克拉码头.
        Stop("singapore_fort_canning", "Fort Canning", "Fort Canning", 1.2945, 103.847,
             note_en="The Malays called it Bukit Larangan, the Forbidden Hill; Stamford Raffles built his house on it.",
             note_it="I malesi la chiamavano Bukit Larangan, la collina proibita; Stamford Raffles vi costruì la sua casa."),
        # Source: Wikipedia, Fort Canning Hill; Wikipedia (de), Fort Canning Park.
        Stop("singapore_raffles_hotel", "Raffles Hotel", "Raffles Hotel", 1.2947, 103.8546,
             note_en="Opened in 1887, the hotel is where the Singapore Sling was invented.",
             note_it="Aperto nel 1887, l'albergo è il luogo dove fu inventato il Singapore Sling."),
        # Source: Wikipedia, Raffles Hotel; Wikipedia (zh), 萊佛士酒店.
        Stop("singapore_st_andrews", "St Andrew's Cathedral", "Cattedrale di Sant'Andrea", 1.2925, 103.8521,
             note_en="The first church here was struck twice by lightning; this cathedral, finished in 1861, took its place.",
             note_it="La prima chiesa qui fu colpita due volte dal fulmine; questa cattedrale, finita nel 1861, ne prese il posto."),
        # Source: Wikipedia, St Andrew's Cathedral, Singapore; Wikipedia (de), St. Andrew’s Cathedral (Singapur).
        Stop("singapore_national_gallery", "National Gallery Singapore", "National Gallery Singapore", 1.2902, 103.8516,
             note_en="Opened in 2015 in the former City Hall and Supreme Court, it is Singapore's largest visual arts venue.",
             note_it="Aperta nel 2015 nell'ex municipio e nell'ex Corte suprema, è il più grande spazio per le arti visive di Singapore."),
        # Source: Wikipedia, National Gallery Singapore; Wikipedia (zh), 新加坡國家美術館.
        Stop("singapore_raffles_landing", "Raffles' Landing Site", "Approdo di Raffles", 1.2877, 103.8507,
             note_en="Stamford Raffles is held to have landed here in 1819; his statue marks the spot.",
             note_it="Si ritiene che Stamford Raffles sia sbarcato qui nel 1819; la sua statua segna il punto."),
        # Source: Wikipedia, Raffles's Landing Site; Wikipedia (de), Thomas Stamford Raffles (his arrival in 1819). The day differs (28 or 29 January): not said.
        Stop("singapore_fullerton", "Fullerton Hotel", "Fullerton Hotel", 1.2862, 103.853,
             note_en="Finished in 1928 as the General Post Office, the building, named after Robert Fullerton, is now a hotel.",
             note_it="Finito nel 1928 come sede delle Poste centrali, l'edificio, intitolato a Robert Fullerton, è oggi un albergo."),
        # Source: Wikipedia, The Fullerton Hotel Singapore; Wikipedia (zh), 富麗敦酒店.
        Stop("singapore_merlion", "Merlion", "Merlion", 1.2868, 103.8545,
             note_en="Half lion, half fish, the Merlion recalls the Lion City and the fishing village Singapore once was; this one is 8.6 metres tall.",
             note_it="Metà leone e metà pesce, il Merlion ricorda la Città del Leone e il villaggio di pescatori che Singapore era un tempo; questo è alto 8,6 metri."),
        # Source: Wikipedia, Merlion; Wikipedia (zh), 鱼尾狮.
        Stop("singapore_esplanade", "Esplanade", "Esplanade", 1.2896, 103.856,
             note_en="Opened in 2002, the theatres' two domes are covered in aluminium sunshades against the tropical sun.",
             note_it="Aperti nel 2002, i teatri hanno due cupole coperte di frangisole d'alluminio contro il sole tropicale."),
        # Source: Wikipedia, Esplanade – Theatres on the Bay; Wikipedia (zh), 濱海藝術中心. Its nickname, the Durian, is in one only: not said.
        Stop("singapore_helix_bridge", "Helix Bridge", "Helix Bridge", 1.2876, 103.8603,
             note_en="Opened in 2010, this footbridge is shaped like DNA; at night it lights up the letters c, g, a and t, its four bases.",
             note_it="Aperto nel 2010, questo ponte pedonale ha la forma del DNA; di notte vi si accendono le lettere c, g, a e t, le sue quattro basi."),
        # Source: Wikipedia, Helix Bridge; Wikipedia (zh), 螺旋桥.
        Stop("singapore_marina_bay_sands", "Marina Bay Sands", "Marina Bay Sands", 1.2837, 103.8605,
             note_en="Designed by Moshe Safdie and opened in 2010, its three towers are joined at the top by the SkyPark and a 150-metre infinity pool.",
             note_it="Progettato da Moshe Safdie e aperto nel 2010, ha tre torri unite in cima dallo SkyPark e da una piscina a sfioro di 150 metri."),
        # Source: Wikipedia, Marina Bay Sands; Wikipedia (zh), 濱海灣金沙.
        Stop("singapore_supertree_grove", "Supertree Grove", "Supertree Grove", 1.2818, 103.8638,
             note_en="In Gardens by the Bay, opened in 2012, the Supertrees are covered in plants and light up every evening to music.",
             note_it="Nei Gardens by the Bay, aperti nel 2012, i Supertree sono coperti di piante e si accendono ogni sera a ritmo di musica."),
        # Source: Wikipedia, Gardens by the Bay; Wikipedia (it), Gardens by the Bay (the Chinese article has nothing on the Supertrees).
    ],
)

BANGKOK = Walk(
    id="BANGKOK_SWING_ARUN",
    city="bangkok",
    city_en="Bangkok",
    city_it="Bangkok",
    route_en="From the Giant Swing to Wat Arun, by the Golden Mount, the Grand Palace and Wat Pho",
    route_it="Dall'Altalena gigante al Wat Arun, passando per il Monte d'oro, il Grande Palazzo e il Wat Pho",
    outing_en="A walk in Bangkok",
    outing_it="Passeggiata a Bangkok",
    country="TH",
    continent="ASIA_OCEANIA",
    # The Chao Phraya, in two areas and many small ones (from the street tiles); the old city's
    # moats and canals as lines.
    water=["relation/14038958", "relation/14038959", "relation/14038956", "relation/1291706"],
    water_from_tiles=True,
    canals=["relation/19051851", "relation/21173153", "relation/20072015"],
    canal_width=15.0,
    parks=[
        "way/23630232", "way/23486019", "relation/19894289", "way/560595626", "way/1552957093",
    ],
    stops=[
        Stop("bangkok_giant_swing", "Giant Swing", "Altalena gigante", 13.7518, 100.5013,
             note_en="In front of Wat Suthat, the swing served a Brahmin ceremony until 1935, when accidents brought it to an end.",
             note_it="Davanti al Wat Suthat, l'altalena serviva a una cerimonia brahmanica fino al 1935, quando gli incidenti la fecero cessare."),
        # Source: Wikipedia, Giant Swing; Wikipedia (de), Sao Ching Cha.
        Stop("bangkok_golden_mount", "Golden Mount", "Monte d'oro", 13.7537, 100.506,
             note_en="This artificial hill, crowned by a golden chedi, rose where a huge chedi begun under Rama III sank into the soft ground.",
             note_it="Questa collina artificiale, coronata da un chedi d'oro, sorse dove un enorme chedi voluto da Rama III sprofondò nel terreno molle."),
        # Source: Wikipedia, Wat Saket; Wikipedia (de), Wat Saket. Its steps differ (344 or 318): not said.
        Stop("bangkok_loha_prasat", "Loha Prasat", "Loha Prasat", 13.7548, 100.5043,
             note_en="Rama III built this temple in 1846 for his granddaughter; it is known for the Loha Prasat, the Iron Palace.",
             note_it="Rama III fece costruire questo tempio nel 1846 per la nipote; è noto per il Loha Prasat, il Palazzo di ferro."),
        # Source: Wikipedia, Wat Ratchanatdaram; Wikipedia (de), Wat Ratchanatdaram.
        Stop("bangkok_democracy_monument", "Democracy Monument", "Monumento alla Democrazia", 13.7567, 100.5018,
             note_en="Raised by Phibun's government, the monument recalls the revolution of 1932, which gave Siam a constitution.",
             note_it="Voluto dal governo di Phibun, il monumento ricorda la rivoluzione del 1932, che diede al Siam una costituzione."),
        # Source: Wikipedia, Democracy Monument; Wikipedia (it), Monumento alla Democrazia.
        Stop("bangkok_khaosan", "Khaosan Road", "Khaosan Road", 13.7589, 100.4973,
             note_en="Its name means milled rice, once sold here; since the 1980s it has been the backpackers' street.",
             note_it="Il nome significa riso brillato, che un tempo si vendeva qui; dagli anni Ottanta è la via dei viaggiatori con lo zaino."),
        # Source: Wikipedia, Khaosan Road; Wikipedia (de), Khaosan Road.
        Stop("bangkok_phra_sumen", "Phra Sumen Fort", "Forte Phra Sumen", 13.7637, 100.4958,
             note_en="Phra Sumen is one of the few left of the fourteen forts that guarded Bangkok's old walls.",
             note_it="Phra Sumen è uno dei pochi rimasti dei quattordici forti che difendevano le antiche mura di Bangkok."),
        # Source: Wikipedia, Fortifications of Bangkok (four remain); Wikipedia (de), Phra Nakhon (two remain): "few".
        Stop("bangkok_national_museum", "National Museum", "Museo nazionale", 13.7578, 100.4925,
             note_en="Founded by King Chulalongkorn in 1874, the museum fills the Front Palace of the old vice-kings, the Wang Na.",
             note_it="Fondato dal re Chulalongkorn nel 1874, il museo occupa il Palazzo anteriore degli antichi viceré, il Wang Na."),
        # Source: Wikipedia, Bangkok National Museum; Wikipedia (de), Nationalmuseum Bangkok.
        Stop("bangkok_wat_mahathat", "Wat Mahathat", "Wat Mahathat", 13.7551, 100.4917,
             note_en="Older than Bangkok, when it was called Wat Salak, the temple now holds a Buddhist university for monks.",
             note_it="Più antico di Bangkok, quando si chiamava Wat Salak, il tempio ospita oggi un'università buddhista per monaci."),
        # Source: Wikipedia, Wat Mahathat Yuwaratrangsarit; Wikipedia (de), Wat Mahathat (Bangkok).
        Stop("bangkok_wat_phra_kaew", "Wat Phra Kaew", "Wat Phra Kaew", 13.7522, 100.4937,
             note_en="In the grounds of the Grand Palace, this is the temple of the Emerald Buddha, the kings' own.",
             note_it="Nel recinto del Grande Palazzo, è il tempio del Buddha di Smeraldo, il tempio dei re."),
        # Source: Wikipedia, Wat Phra Kaew; Wikipedia (de), Wat Phra Kaeo.
        Stop("bangkok_wat_pho", "Wat Pho", "Wat Pho", 13.7465, 100.4935,
             note_en="Its reclining Buddha is 46 metres long, and the temple is a home of traditional Thai massage.",
             note_it="Il suo Buddha disteso è lungo 46 metri, e il tempio è una casa del massaggio tradizionale thailandese."),
        # Source: Wikipedia, Wat Pho; Wikipedia (de), Wat Pho.
        Stop("bangkok_memorial_bridge", "Memorial Bridge", "Ponte Phra Phutthayotfa", 13.7393, 100.4976,
             note_en="Opened in 1932 for the 150th year of the Chakri dynasty, the bridge crosses to Thonburi; its middle once lifted for ships.",
             note_it="Aperto nel 1932 per i 150 anni della dinastia Chakri, il ponte porta a Thonburi; un tempo la parte centrale si alzava per le navi."),
        # Source: Wikipedia, Memorial Bridge (Bangkok); Wikipedia (de), Phra-Phutthayotfa-Brücke. Pak Khlong Talat, the flower market before it, has an article in English only.
        Stop("bangkok_wat_arun", "Wat Arun", "Wat Arun", 13.7437, 100.4895,
             note_en="The Temple of Dawn kept the Emerald Buddha before it crossed the river; its prang is covered in pieces of Chinese porcelain.",
             note_it="Il Tempio dell'Aurora custodì il Buddha di Smeraldo prima che attraversasse il fiume; il suo prang è rivestito di frammenti di porcellana cinese."),
        # Source: Wikipedia, Wat Arun; Wikipedia (de), Wat Arun. The year the Buddha moved differs (1784 or 1785): not said.
    ],
)

KYOTO = Walk(
    id="KYOTO_KIYOMIZU_NISHIKI",
    city="kyoto",
    city_en="Kyoto",
    city_it="Kyoto",
    route_en="From Kiyomizu-dera to the Nishiki market, by Gion and Pontochō",
    route_it="Dal Kiyomizu-dera al mercato di Nishiki, passando per Gion e Pontochō",
    outing_en="A walk in Kyoto",
    outing_it="Passeggiata a Kyoto",
    country="JP",
    continent="ASIA_OCEANIA",
    # A short walk (about 5 km). The Kamo, many small areas, from the street tiles; the Takase
    # and the lake canal as lines.
    water=[],
    water_from_tiles=True,
    canals=["relation/18827840", "relation/9491921"],
    canal_width=8.0,
    # Gion and Higashiyama are mapped mostly as residential lanes: without them the map is a few lines.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    parks=["way/54170783", "relation/9371889"],
    stops=[
        Stop("kyoto_kiyomizu", "Kiyomizu-dera", "Kiyomizu-dera", 34.9963, 135.7826,
             note_en="The temple's great wooden stage, built without a single nail, gave Japan the saying: to jump off the stage at Kiyomizu, to take the plunge.",
             note_it="La grande terrazza di legno del tempio, costruita senza un solo chiodo, ha dato al Giappone il detto: saltare dalla terrazza del Kiyomizu, cioè buttarsi."),
        # Source: Wikipedia, Kiyomizu-dera; Wikipedia (ja), 清水寺.
        Stop("kyoto_sannenzaka", "Sannenzaka", "Sannenzaka", 34.997, 135.7812,
             note_en="On the way up to Kiyomizu, this stepped lane is a protected district of traditional houses.",
             note_it="Sulla salita al Kiyomizu, questa via a gradini è un quartiere protetto di case tradizionali."),
        # Source: Wikipedia, Sannenzaka; Wikipedia (ja), 産寧坂. The year it was protected differs (1972 or 1976): not said.
        Stop("kyoto_yasaka_pagoda", "Yasaka Pagoda", "Pagoda di Yasaka", 34.9985, 135.7795,
             note_en="The five-storey pagoda standing today was rebuilt in 1440, after it had been destroyed more than once.",
             note_it="La pagoda a cinque piani che si vede oggi fu ricostruita nel 1440, dopo essere stata distrutta più volte."),
        # Source: Wikipedia, Yasaka Pagoda; Wikipedia (ja), 法観寺.
        Stop("kyoto_kodaiji", "Kōdai-ji", "Kōdai-ji", 35.001, 135.78,
             note_en="Toyotomi Hideyoshi's widow, become the nun Kōdai-in, founded this temple to pray for his soul.",
             note_it="La vedova di Toyotomi Hideyoshi, divenuta la monaca Kōdai-in, fondò questo tempio per pregare per la sua anima."),
        # Source: Wikipedia, Kōdai-ji; Wikipedia (ja), 高台寺.
        Stop("kyoto_maruyama", "Maruyama Park", "Parco Maruyama", 35.0038, 135.78,
             note_en="Kyoto's best-known park for the cherry blossom is famous for its great weeping cherry.",
             note_it="Il parco più noto di Kyoto per la fioritura dei ciliegi è famoso per il suo grande ciliegio piangente."),
        # Source: Wikipedia, Maruyama Park; Wikipedia (ja), 円山公園 (京都府).
        Stop("kyoto_yasaka_shrine", "Yasaka Shrine", "Santuario di Yasaka", 35.0036, 135.7766,
             note_en="Dedicated to the god Susanoo, the shrine holds the Gion Matsuri every July.",
             note_it="Dedicato al dio Susanoo, il santuario celebra ogni luglio il Gion Matsuri."),
        # Source: Wikipedia, Yasaka Shrine; Wikipedia (ja), 八坂神社.
        Stop("kyoto_hanamikoji", "Hanamikōji", "Hanamikōji", 35.0022, 135.7748,
             note_en="Gion is one of Japan's best-known geisha districts; in Kyoto the geisha are called geiko.",
             note_it="Gion è uno dei quartieri delle geisha più noti del Giappone; a Kyoto le geisha si chiamano geiko."),
        # Source: Wikipedia, Gion; Wikipedia (ja), 祇園. Shirakawa, beyond, is described in the Japanese article only: not a stop.
        Stop("kyoto_kenninji", "Kennin-ji", "Kennin-ji", 35.0003, 135.774,
             note_en="This Zen temple was founded in 1202, with the monk Eisai as its first abbot.",
             note_it="Questo tempio zen fu fondato nel 1202, con il monaco Eisai come primo abate."),
        # Source: Wikipedia, Kennin-ji; Wikipedia (ja), 建仁寺.
        Stop("kyoto_pontocho", "Pontochō", "Pontochō", 35.0042, 135.7712,
             note_en="This narrow lane by the Kamo River may take its name from Portuguese, and is one of Kyoto's geisha districts.",
             note_it="Questo vicolo stretto lungo il fiume Kamo deve forse il nome al portoghese, ed è uno dei quartieri delle geisha di Kyoto."),
        # Source: Wikipedia, Ponto-chō; Wikipedia (ja), 先斗町. Both give the Portuguese as a theory.
        Stop("kyoto_nishiki", "Nishiki Market", "Mercato di Nishiki", 35.005, 135.766,
             note_en="Called Kyoto's kitchen, the market grew from fish shops that kept their fish fresh in the cold groundwater, for the palace.",
             note_it="Detto «la cucina di Kyoto», il mercato nacque da botteghe di pesce che lo tenevano fresco nell'acqua fredda del sottosuolo, per il palazzo."),
        # Source: Wikipedia, Nishiki Market; Wikipedia (ja), 錦市場.
    ],
)

HANOI = Walk(
    id="HANOI_VAN_MIEU_LONG_BIEN",
    city="hanoi",
    city_en="Hanoi",
    city_it="Hanoi",
    route_en="From the Temple of Literature to the Long Biên Bridge, by Hoàn Kiếm Lake and the Old Quarter",
    route_it="Dal Tempio della Letteratura al ponte Long Biên, passando per il lago Hoàn Kiếm e il Quartiere vecchio",
    outing_en="A walk in Hanoi",
    outing_it="Passeggiata a Hanoi",
    country="VN",
    continent="ASIA_OCEANIA",
    # A short walk (about 5 km). Hoàn Kiếm Lake; the Red River.
    water=["relation/198437", "relation/6907107"],
    # The Long Biên Bridge's deck is mapped as a residential street, and so are many of the Old
    # Quarter's: the longer ones drawn, so the bridge reads as one over the river.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    parks=[
        "relation/21390321", "relation/21422526", "relation/21422527", "way/218414273", "relation/14048553",
    ],
    stops=[
        Stop("hanoi_van_mieu", "Temple of Literature", "Tempio della Letteratura", 21.0285, 105.837,
             note_en="Founded in 1070 for Confucius, the temple keeps stelae on stone turtles with the names of the scholars who passed the royal exams.",
             note_it="Fondato nel 1070 in onore di Confucio, il tempio conserva stele su tartarughe di pietra con i nomi dei dotti promossi agli esami reali."),
        # Source: Wikipedia, Temple of Literature, Hanoi; Wikipedia (fr), Temple de la Littérature de Hanoï. When its teaching ended differs (1779 or 1915): not said.
        Stop("hanoi_hoa_lo", "Hỏa Lò Prison", "Prigione di Hỏa Lò", 21.0254, 105.8465,
             note_en="Built by the French colonial government, the prison later held American prisoners of war, who called it the Hanoi Hilton; part of it is now a museum.",
             note_it="Costruita dall'amministrazione coloniale francese, la prigione rinchiuse poi prigionieri di guerra americani, che la chiamarono Hanoi Hilton; oggi in parte è un museo."),
        # Source: Wikipedia, Hỏa Lò Prison; Wikipedia (fr), Prison Hỏa Lò.
        Stop("hanoi_st_joseph", "St Joseph's Cathedral", "Cattedrale di San Giuseppe", 21.0285, 105.8489,
             note_en="Finished in 1886 in the Gothic Revival style, the cathedral is said to resemble Notre-Dame de Paris.",
             note_it="Finita nel 1886 in stile neogotico, la cattedrale ricorderebbe Notre-Dame di Parigi."),
        # Source: Wikipedia, St. Joseph's Cathedral, Hanoi; Wikipedia (fr), Cathédrale Saint-Joseph de Hanoï.
        Stop("hanoi_hoan_kiem", "Hoàn Kiếm Lake", "Lago Hoàn Kiếm", 21.0279, 105.8516,
             note_en="Here, the legend says, Emperor Lê Lợi gave his magic sword back to a turtle, after he had driven out the Chinese.",
             note_it="Qui, dice la leggenda, l'imperatore Lê Lợi restituì la sua spada magica a una tartaruga, dopo aver cacciato i cinesi."),
        # Source: Wikipedia, Hoàn Kiếm Lake; Wikipedia (fr), Lac Hoan Kiem. The turtle is golden in one only: not said.
        Stop("hanoi_ngoc_son", "Ngọc Sơn Temple", "Tempio di Ngọc Sơn", 21.0311, 105.8528,
             note_en="The Thê Húc bridge leads to this temple on its islet, which honours, among others, the national hero Trần Hưng Đạo.",
             note_it="Il ponte Thê Húc porta a questo tempio sul suo isolotto, che onora tra gli altri l'eroe nazionale Trần Hưng Đạo."),
        # Source: Wikipedia, Ngọc Sơn Temple; Wikipedia (fr), Temple Ngoc Son.
        Stop("hanoi_old_quarter", "Old Quarter", "Quartiere vecchio", 21.0359, 105.8511,
             note_en="Known as the 36 streets, the Old Quarter once gave each street its own trade.",
             note_it="Detto le 36 strade, il Quartiere vecchio dava un tempo a ogni via il suo mestiere."),
        # Source: Wikipedia, Old Quarter, Hanoi; Wikipedia (it), Quartiere vecchio di Hanoi. The Bạch Mã temple, here, has an article in Vietnamese only.
        Stop("hanoi_dong_xuan", "Đồng Xuân Market", "Mercato di Đồng Xuân", 21.038, 105.8497,
             note_en="Built by the French in place of two older markets, it was nearly destroyed by fire in 1994.",
             note_it="Costruito dai francesi al posto di due mercati più antichi, fu quasi distrutto da un incendio nel 1994."),
        # Source: Wikipedia, Đồng Xuân Market; Wikipedia (fr), Marché Đồng Xuân.
        Stop("hanoi_long_bien", "Long Biên Bridge", "Ponte Long Biên", 21.044, 105.8605,
             note_en="Built by Daydé & Pillé of Paris and first named after Paul Doumer, the bridge carries the railway over the Red River.",
             note_it="Costruito dalla ditta parigina Daydé & Pillé e chiamato all'inizio come Paul Doumer, il ponte porta la ferrovia oltre il fiume Rosso."),
        # Source: Wikipedia, Long Biên Bridge; Wikipedia (fr), Pont Long Biên. The year it was finished differs (1902 or 1903): not said.
        # The stop is on the bridge, over the river, so the walk ends where the map shows a bridge.
    ],
)

MELBOURNE = Walk(
    id="MELBOURNE_FLINDERS_EXHIBITION",
    city="melbourne",
    city_en="Melbourne",
    city_it="Melbourne",
    route_en="From Flinders Street to the Royal Exhibition Building, by the State Library and Parliament",
    route_it="Da Flinders Street al Royal Exhibition Building, passando per la State Library e il Parlamento",
    outing_en="A walk in Melbourne",
    outing_it="Passeggiata a Melbourne",
    country="AU",
    continent="ASIA_OCEANIA",
    # A short walk (about 5 km). The Yarra.
    water=["relation/954522"],
    # The gardens along the Yarra, the Treasury and Fitzroy Gardens, the Carlton Gardens; the
    # smaller ones the walk passes: Parliament's gardens, Gordon Reserve, the squares to the north.
    parks=[
        "way/46330961", "way/23909867", "way/24593719", "way/24593825", "way/4817097", "way/4817020",
        "relation/6614802", "way/222848213", "way/27783990", "way/46142201", "way/510918356",
        "way/32943432", "way/154434398", "way/177499279", "way/4817077",
    ],
    stops=[
        Stop("melbourne_flinders_street", "Flinders Street Station", "Stazione di Flinders Street", -37.818, 144.9669,
             note_en="The first station here served Australia's first railway in 1854; the clocks over the entrance show each line's next train.",
             note_it="La prima stazione qui servì nel 1854 la prima ferrovia d'Australia; gli orologi sopra l'ingresso indicano il prossimo treno di ogni linea."),
        # Source: Wikipedia, Flinders Street railway station; Wikipedia (de), Bahnhof Melbourne Flinders Street.
        Stop("melbourne_st_pauls", "St Paul's Cathedral", "Cattedrale di San Paolo", -37.817, 144.9676,
             note_en="William Butterfield's cathedral of 1891 stands where Melbourne's first public Christian service was held, in 1835.",
             note_it="La cattedrale di William Butterfield, del 1891, sorge dove nel 1835 si tenne la prima funzione cristiana pubblica di Melbourne."),
        # Source: Wikipedia, St Paul's Cathedral, Melbourne; Wikipedia (de), Pauluskathedrale (Melbourne).
        Stop("melbourne_federation_square", "Federation Square", "Federation Square", -37.8179, 144.969,
             note_en="Opened in 2002, the square and its galleries stand on a concrete deck above the railway lines.",
             note_it="Aperta nel 2002, la piazza con le sue gallerie poggia su una piattaforma di cemento sopra i binari."),
        # Source: Wikipedia, Federation Square; Wikipedia (fr), Federation Square. Hosier Lane, the Block Arcade and the GPO, on the way, have articles in English only.
        Stop("melbourne_state_library", "State Library Victoria", "State Library Victoria", -37.8098, 144.965,
             note_en="Among the library's treasures is the armour of the bushranger Ned Kelly.",
             note_it="Tra i tesori della biblioteca c'è l'armatura del bandito Ned Kelly."),
        # Source: Wikipedia, State Library Victoria; Wikipedia (de), State Library of Victoria. Its first year differs (1854 or 1856): not said.
        Stop("melbourne_old_gaol", "Old Melbourne Gaol", "Old Melbourne Gaol", -37.8077, 144.9655,
             note_en="Ned Kelly was among the prisoners of this gaol, closed in 1924 and now a museum.",
             note_it="Ned Kelly fu tra i detenuti di questo carcere, chiuso nel 1924 e oggi museo."),
        # Source: Wikipedia, Old Melbourne Gaol; Wikipedia (de), Old Melbourne Gaol. The number hanged differs (133 or 135): not said.
        Stop("melbourne_princess_theatre", "Princess Theatre", "Princess Theatre", -37.8108, 144.9727,
             note_en="Rebuilt in 1886 to William Pitt's design, the theatre had the world's first sliding roof.",
             note_it="Ricostruito nel 1886 su progetto di William Pitt, il teatro ebbe il primo tetto scorrevole al mondo."),
        # Source: Wikipedia, Princess Theatre (Melbourne); Wikipedia (de), Princess Theatre (Melbourne).
        Stop("melbourne_parliament", "Parliament House", "Parlamento", -37.811, 144.9733,
             note_en="Australia's parliament sat here while Melbourne was the capital; the dome its builders planned was never built.",
             note_it="Qui si riunì il parlamento australiano quando Melbourne era la capitale; la cupola prevista non fu mai costruita."),
        # Source: Wikipedia, Parliament House, Melbourne; Wikipedia (de), Parliament House (Melbourne). The year it left differs (1927 or 1928): not said.
        Stop("melbourne_st_patricks", "St Patrick's Cathedral", "Cattedrale di San Patrizio", -37.8099, 144.9765,
             note_en="This Gothic Revival cathedral is by William Wardell, the architect of Sydney's St Mary's.",
             note_it="Questa cattedrale neogotica è di William Wardell, l'architetto della cattedrale di Santa Maria a Sydney."),
        # Source: Wikipedia, St Patrick's Cathedral, Melbourne; Wikipedia (de), St. Patrick’s Cathedral (Melbourne); for St Mary's, the Sydney walk's sources.
        Stop("melbourne_exhibition", "Royal Exhibition Building", "Royal Exhibition Building", -37.8055, 144.9715,
             note_en="Built for the international exhibition of 1880, the hall and its gardens have been a World Heritage Site since 2004.",
             note_it="Costruito per l'esposizione internazionale del 1880, il palazzo con i suoi giardini è patrimonio dell'umanità dal 2004."),
        # Source: Wikipedia, Royal Exhibition Building; Wikipedia (de), Royal Exhibition Building.
    ],
)

CAIRO = Walk(
    id="CAIRO_MUSEUM_CITADEL",
    city="cairo",
    city_en="Cairo",
    city_it="Il Cairo",
    route_en="From the Egyptian Museum to the Citadel, by Al-Muizz Street and Al-Azhar",
    route_it="Dal Museo Egizio alla Cittadella, passando per via al-Muizz e al-Azhar",
    outing_en="A walk in Cairo",
    outing_it="Passeggiata al Cairo",
    country="EG",
    continent="AFRICA",
    # The medieval city's lanes are mapped as residential streets.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=400,
    # The Nile.
    water=["relation/2063663"],
    # Al-Azhar Park, the Ezbekiyya Garden.
    parks=["way/24745061", "way/99842004"],
    stops=[
        Stop("cairo_egyptian_museum", "Egyptian Museum", "Museo Egizio", 30.0476, 31.2338,
             note_en="Opened on Tahrir Square in 1902, the museum keeps in its garden the tomb of Auguste Mariette, its first curator.",
             note_it="Aperto su piazza Tahrir nel 1902, il museo custodisce nel suo giardino la tomba di Auguste Mariette, il suo primo conservatore."),
        # Source: Wikipedia, Egyptian Museum (Mariette's museum at Boulaq, 1858; his tomb moved to the garden in 1902); Wikipedia (fr), Musée égyptien du Caire (premier conservateur).
        Stop("cairo_talaat_harb", "Talaat Harb Square", "Piazza Talaat Harb", 30.0476, 31.2385,
             note_en="The statue in the middle of the square is of Talaat Harb, who founded Banque Misr in 1920.",
             note_it="La statua al centro della piazza è di Talaat Harb, che nel 1920 fondò la Banque Misr."),
        # Source: Wikipedia, Talaat Harb; Wikipedia (ar), ميدان طلعت حرب, and طلعت حرب. The square has an article in Arabic only: its old name (Suleiman Pasha) is not said.
        Stop("cairo_abdeen", "Abdeen Palace", "Palazzo Abdeen", 30.0424, 31.2462,
             note_en="Begun in 1863 and inaugurated in 1874 on land of Abdeen Bey, whose name it keeps, the palace replaced the Citadel as the seat of power.",
             note_it="Iniziato nel 1863 e inaugurato nel 1874 su un terreno di Abdeen Bey, di cui porta il nome, il palazzo prese il posto della Cittadella come sede del potere."),
        # Source: Wikipedia, Abdeen Palace; Wikipedia (fr), Palais d'Abedin.
        Stop("cairo_islamic_art", "Museum of Islamic Art", "Museo d'arte islamica", 30.0448, 31.2527,
             note_en="Its collection began in the ruined mosque of al-Hakim, further on; this neo-Mamluk building was finished in 1902.",
             note_it="La sua collezione nacque nella moschea in rovina di al-Hakim, più avanti sul percorso; questo edificio neomamelucco fu finito nel 1902."),
        # Source: Wikipedia, Museum of Islamic Art, Cairo; Wikipedia (de), Museum für Islamische Kunst (Kairo). The year it opened differs (1902 or 1903): not said.
        Stop("cairo_qalawun", "Qalawun complex", "Complesso di Qalawun", 30.0495, 31.2610,
             note_en="Sultan Qalawun's hospital, madrasa and mausoleum date from 1284 and 1285; the complex is said to have been built in just thirteen months.",
             note_it="L'ospedale, la madrasa e il mausoleo del sultano Qalawun risalgono al 1284 e 1285; si dice che il complesso sia stato costruito in soli tredici mesi."),
        # Source: Wikipedia, Qalawun complex ("reportedly"); Wikipedia (de), Grabkomplex des Qalawun ("soll").
        Stop("cairo_aqmar", "Aqmar Mosque", "Moschea al-Aqmar", 30.0516, 31.2620,
             note_en="Built in 1125 under the Fatimids, it was one of Cairo's first mosques whose façade follows the street, while its inside faces Mecca.",
             note_it="Costruita nel 1125 sotto i Fatimidi, fu una delle prime moschee del Cairo con la facciata allineata alla strada, mentre l'interno è rivolto alla Mecca."),
        # Source: Wikipedia, Aqmar Mosque (the first); Wikipedia (fr), Mosquée al-Aqmar (one of the first): the sentence says one of the first.
        Stop("cairo_al_hakim", "Al-Hakim Mosque", "Moschea di al-Hakim", 30.0545, 31.2634,
             note_en="Begun in 990 and finished in 1013 by the caliph al-Hakim, whose name it bears, the mosque was restored in 1980 by the Dawoodi Bohras.",
             note_it="Iniziata nel 990 e finita nel 1013 dal califfo al-Hakim, di cui porta il nome, la moschea fu restaurata nel 1980 dai Dawoodi Bohra."),
        # Source: Wikipedia, Al-Hakim Mosque; Wikipedia (fr), Mosquée Al-Hakim.
        Stop("cairo_bab_al_nasr", "Bab al-Nasr", "Bab al-Nasr", 30.0542, 31.2650,
             note_en="The Gate of Victory, built by the vizier Badr al-Jamali in 1087, has names of Napoleon's officers carved near its top.",
             note_it="La Porta della Vittoria, costruita dal visir Badr al-Jamali nel 1087, porta incisi in alto i nomi di ufficiali di Napoleone."),
        # Source: Wikipedia, Bab al-Nasr (Cairo); Wikipedia (fr), Bab al-Nasr (Le Caire).
        Stop("cairo_khan_el_khalili", "Khan el-Khalili", "Khan el-Khalili", 30.0473, 31.2623,
             note_en="The bazaar is named after a caravanserai built by the emir Jaharkas al-Khalili; Naguib Mahfouz set Midaq Alley here.",
             note_it="Il bazar prende il nome da un caravanserraglio costruito dall'emiro Jaharkas al-Khalili; qui Nagib Mahfuz ambientò Vicolo del mortaio."),
        # Source: Wikipedia, Khan el-Khalili; Wikipedia (it), Khan el-Khalili. The stop is on the bazaar's side of al-Hussein's square.
        Stop("cairo_al_azhar", "Al-Azhar Mosque", "Moschea di al-Azhar", 30.0456, 31.2622,
             note_en="Built by the Fatimid general Jawhar al-Siqilli when he founded Cairo, the mosque is the seat of Al-Azhar University.",
             note_it="Costruita dal generale fatimide Jawhar al-Siqilli quando fondò il Cairo, la moschea è la sede dell'Università di al-Azhar."),
        # Source: Wikipedia, Al-Azhar Mosque; Wikipedia (fr), Mosquée Al-Azhar. Which universities are older differs: not said.
        Stop("cairo_al_ghuri", "Al-Ghuri complex", "Complesso di al-Ghuri", 30.0459, 31.2598,
             note_en="Sultan al-Ghuri built his mosque and his mausoleum face to face across the street, but after the battle of Marj Dabiq, in 1516, his body was never found.",
             note_it="Il sultano al-Ghuri costruì moschea e mausoleo uno di fronte all'altro sulla strada, ma dopo la battaglia di Marj Dabiq, nel 1516, il suo corpo non fu mai trovato."),
        # Source: Wikipedia, Sultan al-Ghuri Complex; Wikipedia (ar), مجموعة السلطان الأشرف الغوري.
        Stop("cairo_bab_zuwayla", "Bab Zuwayla", "Bab Zuwayla", 30.0428, 31.2579,
             note_en="The southern gate of the Fatimid walls, built in 1092, bears on its towers the two minarets of the mosque of al-Mu'ayyad, added in the 15th century.",
             note_it="Porta meridionale delle mura fatimide, costruita nel 1092, regge sulle sue torri i due minareti della moschea di al-Mu'ayyad, aggiunti nel Quattrocento."),
        # Source: Wikipedia, Bab Zuwayla; Wikipedia (fr), Bab Zuweila.
        Stop("cairo_blue_mosque", "Blue Mosque", "Moschea Blu", 30.0362, 31.2604,
             note_en="Finished in 1347 for the emir Aqsunqur, it is called the Blue Mosque for the tiles a Janissary, Ibrahim Agha, added in the 17th century.",
             note_it="Finita nel 1347 per l'emiro Aqsunqur, è detta Moschea Blu per le piastrelle che un giannizzero, Ibrahim Agha, vi aggiunse nel Seicento."),
        # Source: Wikipedia, Aqsunqur Mosque; Wikipedia (de), Aqsunqur-Moschee. The years of the tiles differ (1652 to 1654, or to 1664): the century is said.
        Stop("cairo_sultan_hasan", "Sultan Hasan Mosque", "Moschea del sultano Hasan", 30.0324, 31.2562,
             note_en="Begun by Sultan Hasan in 1356, the madrasa taught the four schools of Sunni law, each, it is said, in one of the great iwans around its courtyard.",
             note_it="Iniziata dal sultano Hasan nel 1356, la madrasa insegnava le quattro scuole del diritto sunnita, ciascuna, si dice, in uno dei grandi iwan intorno al cortile."),
        # Source: Wikipedia, Mosque-Madrasa of Sultan Hasan ("said to have"); Wikipedia (fr), Mosquée du sultan Hassan.
        Stop("cairo_muhammad_ali", "Muhammad Ali Mosque", "Moschea di Muhammad Ali", 30.0291, 31.2598,
             note_en="In Saladin's Citadel, Muhammad Ali's mosque holds his tomb and a clock sent by the King of the French in return for the obelisk of Luxor, now in Paris.",
             note_it="Nella Cittadella di Saladino, la moschea di Muhammad Ali custodisce la sua tomba e un orologio donato dal re dei Francesi in cambio dell'obelisco di Luxor, oggi a Parigi."),
        # Source: Wikipedia, Muhammad Ali Mosque; Wikipedia (fr), Mosquée Mohammed Ali.
    ],
)

CAPE_TOWN = Walk(
    id="CAPE_TOWN_LIGHTHOUSE_BO_KAAP",
    city="cape_town",
    city_en="Cape Town",
    city_it="Città del Capo",
    route_en="From the Green Point Lighthouse to Bo-Kaap, by the Waterfront, the Castle and the Company's Garden",
    route_it="Dal faro di Green Point al Bo-Kaap, passando per il Waterfront, il Castello e i Company's Garden",
    outing_en="A walk in Cape Town",
    outing_it="Passeggiata a Città del Capo",
    country="ZA",
    continent="AFRICA",
    # Table Bay, from the coastline; the Waterfront's basins.
    coast=True,
    water=["relation/15602639", "relation/15602640", "relation/15602641"],
    # Green Point Park and its gardens, the Company's Garden.
    parks=["way/44948367", "relation/9636593", "relation/9636594", "way/8035472"],
    stops=[
        Stop("cape_town_lighthouse", "Green Point Lighthouse", "Faro di Green Point", -33.9014, 18.3999,
             note_en="First lit in 1824, South Africa's oldest lighthouse has had a foghorn since 1926.",
             note_it="Acceso per la prima volta nel 1824, il faro più antico del Sudafrica ha una sirena da nebbia dal 1926."),
        # Source: Wikipedia, Green Point Lighthouse, Cape Town; Wikipedia (af), Groenpunt-vuurtoring. Its cost differs: not said.
        Stop("cape_town_stadium", "Cape Town Stadium", "Stadio di Città del Capo", -33.9045, 18.4105,
             note_en="Built for the 2010 football World Cup, the stadium hosted one of its quarter-finals and a semi-final.",
             note_it="Costruito per i Mondiali di calcio del 2010, lo stadio ne ospitò un quarto di finale e una semifinale."),
        # Source: Wikipedia, Cape Town Stadium; Wikipedia (de), Kapstadt-Stadion. Its seats differ: not said.
        Stop("cape_town_waterfront", "V&A Waterfront", "V&A Waterfront", -33.9065, 18.4222,
             note_en="Its two basins are named after Queen Victoria and her son Prince Alfred, who began the harbour's breakwater in 1860.",
             note_it="I suoi due bacini portano il nome della regina Vittoria e di suo figlio, il principe Alfredo, che nel 1860 diede inizio al frangiflutti del porto."),
        # Source: Wikipedia, V&A Waterfront; Wikipedia (de), Victoria & Alfred Waterfront. The stop is the Clock Tower.
        Stop("cape_town_nobel_square", "Nobel Square", "Nobel Square", -33.9055, 18.4195,
             note_en="Its four statues are South Africa's Nobel Peace Prize winners, Albert Luthuli, Desmond Tutu, F. W. de Klerk and Nelson Mandela, with Table Mountain behind them.",
             note_it="Le sue quattro statue sono i premi Nobel per la pace del Sudafrica, Albert Luthuli, Desmond Tutu, F. W. de Klerk e Nelson Mandela, con alle spalle la Montagna della Tavola."),
        # Source: Wikipedia, Nobel Square; Wikipedia (de), Nobel Square.
        Stop("cape_town_foreshore", "Foreshore", "Foreshore", -33.9174, 18.4232,
             note_en="This district was built on land won from Table Bay in the 1930s and 1940s: the Castle, ahead, once stood on the shore.",
             note_it="Questo quartiere sorge su terra strappata alla baia della Tavola negli anni Trenta e Quaranta: il Castello, più avanti, un tempo era sulla riva."),
        # Source: Wikipedia, Foreshore, Cape Town, and Castle of Good Hope (on the coastline before the reclamation); Wikipedia (fr), Foreshore; Wikipedia (nl), Kasteel de Goede Hoop (its first gate faced the sea).
        Stop("cape_town_castle", "Castle of Good Hope", "Castello di Buona Speranza", -33.9259, 18.4267,
             note_en="Its first stone laid in 1666, the Dutch East India Company's fort has five bastions named after the titles of the Prince of Orange.",
             note_it="Posata la prima pietra nel 1666, il forte della Compagnia olandese delle Indie orientali ha cinque bastioni che portano i titoli del principe d'Orange."),
        # Source: Wikipedia, Castle of Good Hope; Wikipedia (nl), Kasteel de Goede Hoop. Whether it is the oldest building differs: not said.
        Stop("cape_town_city_hall", "City Hall", "Municipio", -33.9254, 18.4237,
             note_en="Hours after his release, on 11 February 1990, Nelson Mandela made his first public speech from this balcony; a statue of him has stood there since 2018.",
             note_it="Poche ore dopo la sua liberazione, l'11 febbraio 1990, Nelson Mandela tenne da questo balcone il suo primo discorso pubblico; dal 2018 vi sorge una sua statua."),
        # Source: Wikipedia, Cape Town City Hall; Wikipedia (de), Cape Town City Hall.
        Stop("cape_town_district_six", "District Six Museum", "Museo di District Six", -33.9278, 18.4238,
             note_en="Opened in 1994, the museum remembers the 60,000 people forced to leave District Six under apartheid, in the 1970s.",
             note_it="Aperto nel 1994, il museo ricorda le 60.000 persone costrette a lasciare District Six sotto l'apartheid, negli anni Settanta."),
        # Source: Wikipedia, District Six Museum; Wikipedia (nl), District Six Museum.
        Stop("cape_town_south_african_museum", "South African Museum", "South African Museum", -33.9289, 18.4149,
             note_en="Founded in 1825, the museum has stood in the Company's Garden since 1897; among its collections are whale skeletons.",
             note_it="Fondato nel 1825, il museo è nei Company's Garden dal 1897; tra le sue collezioni ci sono scheletri di balena."),
        # Source: Wikipedia, Iziko South African Museum; Wikipedia (de), Iziko South African Museum.
        Stop("cape_town_company_garden", "Company's Garden", "Company's Garden", -33.9268, 18.4178,
             note_en="Laid out by the first European settlers to grow fresh food for ships rounding the Cape, the garden keeps South Africa's oldest cultivated pear tree, from about 1652.",
             note_it="Creato dai primi coloni europei per coltivare cibo fresco per le navi che doppiavano il Capo, il giardino conserva il più antico pero coltivato del Sudafrica, del 1652 circa."),
        # Source: Wikipedia, Company's Garden; Wikipedia (nl), Company's Garden. When it was laid out differs (the 1650s, or 1650): not said.
        Stop("cape_town_st_georges", "St George's Cathedral", "Cattedrale di San Giorgio", -33.9249, 18.4194,
             note_en="Begun in 1901 to Herbert Baker's design, the cathedral holds the remains of Archbishop Desmond Tutu, before its high altar.",
             note_it="Iniziata nel 1901 su progetto di Herbert Baker, la cattedrale custodisce davanti all'altare maggiore i resti dell'arcivescovo Desmond Tutu."),
        # Source: Wikipedia, St. George's Cathedral, Cape Town; Wikipedia (de), St George’s Cathedral (Kapstadt).
        Stop("cape_town_slave_lodge", "Slave Lodge", "Slave Lodge", -33.9252, 18.4208,
             note_en="Built by the Dutch East India Company in 1679 to house the people it enslaved, the lodge served as such until 1811.",
             note_it="Costruita nel 1679 dalla Compagnia olandese delle Indie orientali per alloggiare le persone che teneva in schiavitù, la Lodge servì a questo fino al 1811."),
        # Source: Wikipedia, Slave Lodge, Cape Town; Wikipedia (de), Iziko Slave Lodge.
        Stop("cape_town_greenmarket", "Greenmarket Square", "Greenmarket Square", -33.9227, 18.4201,
             note_en="Laid out in 1696, the square was a slave market and a vegetable market; the front of its Old Town House is held to be the city's historic centre.",
             note_it="Sorta nel 1696, la piazza fu mercato degli schiavi e mercato della verdura; lo spazio davanti all'Old Town House è considerato il centro storico della città."),
        # Source: Wikipedia, Greenmarket Square; Wikipedia (fr), Place du Marché Vert.
        Stop("cape_town_auwal", "Auwal Mosque", "Moschea Auwal", -33.9223, 18.4150,
             note_en="Held to be South Africa's oldest mosque, from 1794, it had as its first imam Tuan Guru, who had written out the Qur'an from memory in prison.",
             note_it="Considerata la più antica moschea del Sudafrica, del 1794, ebbe come primo imam Tuan Guru, che in prigione aveva trascritto il Corano a memoria."),
        # Source: Wikipedia, Auwal Mosque (the first); Wikipedia (de), Auwal-Moschee ("gilt als" the oldest).
        Stop("cape_town_bo_kaap", "Bo-Kaap", "Bo-Kaap", -33.9205, 18.4128,
             note_en="Bo-Kaap, Afrikaans for above the Cape, is the old Malay Quarter on the slopes of Signal Hill, known for its brightly painted houses.",
             note_it="Il Bo-Kaap, in afrikaans «sopra il Capo», è l'antico Quartiere malese sulle pendici del Signal Hill, noto per le sue case dai colori vivaci."),
        # Source: Wikipedia, Bo-Kaap; Wikipedia (de), Bo-Kaap.
    ],
)

MARRAKECH = Walk(
    id="MARRAKECH_MAJORELLE_SI_SAID",
    city="marrakech",
    city_en="Marrakech",
    city_it="Marrakech",
    route_en="From the Majorelle Garden to Dar Si Said, by the souks, Jemaa el-Fnaa and the Bahia Palace",
    route_it="Dal Giardino Majorelle a Dar Si Said, passando per i souk, Jemaa el-Fnaa e il Palazzo della Bahia",
    outing_en="A walk in Marrakech",
    outing_it="Passeggiata a Marrakech",
    country="MA",
    continent="AFRICA",
    # The medina's lanes are mapped as residential streets.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=200,
    # The Majorelle Garden, the Cyber Park, Lalla Hasna Park, the Koutoubia Gardens.
    parks=["way/41922120", "way/126302813", "way/364290838", "way/435129470"],
    stops=[
        Stop("marrakech_majorelle", "Majorelle Garden", "Giardino Majorelle", 31.6415, -8.0029,
             note_en="Begun by the painter Jacques Majorelle in 1923, the garden is painted in the blue named after him; Yves Saint Laurent and Pierre Bergé later bought it.",
             note_it="Iniziato dal pittore Jacques Majorelle nel 1923, il giardino è dipinto nel blu che porta il suo nome; in seguito lo comprarono Yves Saint Laurent e Pierre Bergé."),
        # Source: Wikipedia, Majorelle Garden (in the 1980s); Wikipedia (fr), Jardin Majorelle (1980).
        Stop("marrakech_bab_doukkala", "Bab Doukkala", "Bab Doukkala", 31.6340, -7.9990,
             note_en="Already there under the Almoravids, the gate takes its name from the Doukkala, the region it led to.",
             note_it="Già presente sotto gli Almoravidi, la porta prende il nome dalla Doukkala, la regione verso cui conduceva."),
        # Source: Wikipedia, Bab Doukkala; Wikipedia (fr), Bab Doukkala (Marrakech).
        Stop("marrakech_dar_el_bacha", "Dar el Bacha", "Dar el Bacha", 31.6315, -7.9929,
             note_en="Built in 1910, the house of the pasha was the home of Thami El Glaoui, pasha of Marrakech; today it is the Museum of Confluences.",
             note_it="Costruita nel 1910, la casa del pascià fu la residenza di Thami El Glaoui, pascià di Marrakech; oggi è il Museo delle Confluenze."),
        # Source: Wikipedia, Dar el Bacha; Wikipedia (fr), Dar el Bacha.
        Stop("marrakech_mouassine", "Mouassine Mosque", "Moschea Mouassine", 31.6299, -7.9893,
             note_en="The Saadian sultan Abdallah al-Ghalib built this mosque, with a fountain, a hammam and a library, on land left free when the Jews were moved to the new mellah.",
             note_it="Il sultano saadiano Abdallah al-Ghalib costruì questa moschea, con una fontana, un hammam e una biblioteca, su terreni lasciati liberi quando gli ebrei furono trasferiti nel nuovo mellah."),
        # Source: Wikipedia, Mouassine Mosque; Wikipedia (fr), Mosquée El Mouassine. The stop is the Mouassine Fountain.
        Stop("marrakech_almoravid_koubba", "Almoravid Koubba", "Qubba almoravide", 31.6315, -7.9872,
             note_en="Built by the Almoravid ruler Ali ibn Yusuf, this dome housed the ablutions for the mosque nearby, of which it is all that is left.",
             note_it="Costruita dal sovrano almoravide Ali ibn Yusuf, questa cupola ospitava le abluzioni per la moschea vicina, di cui è tutto ciò che resta."),
        # Source: Wikipedia, Almoravid Qubba; Wikipedia (fr), Qoubba almoravide, and Médersa Ben Youssef (only the Qoubba is left of the old mosque). Its year differs (1117 or 1125): not said.
        Stop("marrakech_ben_youssef", "Ben Youssef Madrasa", "Madrasa Ben Youssef", 31.6320, -7.9860,
             note_en="Completed by the Saadian sultan Abdallah al-Ghalib in 1564 and 1565, the madrasa lodged its students in small rooms around its courtyard.",
             note_it="Completata dal sultano saadiano Abdallah al-Ghalib nel 1564 e 1565, la madrasa ospitava i suoi studenti in piccole stanze intorno al cortile."),
        # Source: Wikipedia, Ben Youssef Madrasa; Wikipedia (fr), Médersa Ben Youssef.
        Stop("marrakech_jemaa_el_fnaa", "Jemaa el-Fnaa", "Jemaa el-Fnaa", 31.6258, -7.9889,
             note_en="Proclaimed by UNESCO as intangible heritage in 2001, the square has snake charmers and storytellers by day, and fills with food stalls at night.",
             note_it="Proclamata dall'UNESCO patrimonio immateriale nel 2001, la piazza ha incantatori di serpenti e cantastorie di giorno, e la sera si riempie di bancarelle di cibo."),
        # Source: Wikipedia, Jemaa el-Fnaa; Wikipedia (fr), Place Jemaa el-Fna. What its name means differs: not said.
        Stop("marrakech_koutoubia", "Koutoubia Mosque", "Moschea Koutoubia", 31.6238, -7.9934,
             note_en="Its name comes from the Arabic for booksellers; its Almohad minaret likely inspired Seville's Giralda and Rabat's Hassan Tower.",
             note_it="Il suo nome viene dalla parola araba per librai; il suo minareto almohade ispirò probabilmente la Giralda di Siviglia e la Torre Hassan di Rabat."),
        # Source: Wikipedia, Kutubiyya Mosque; Wikipedia (fr), Mosquée Koutoubia. The years differ: not said.
        Stop("marrakech_bab_agnaou", "Bab Agnaou", "Bab Agnaou", 31.6174, -7.9906,
             note_en="Built under the Almohads, the gate led into the royal kasbah of Ya'qub al-Mansur; its name is thought to come from a Berber word for the mute.",
             note_it="Costruita sotto gli Almohadi, la porta conduceva nella kasbah reale di Ya'qub al-Mansur; il suo nome verrebbe da una parola berbera che significa muto."),
        # Source: Wikipedia, Bab Agnaou; Wikipedia (fr), Bab Agnaou.
        Stop("marrakech_saadian_tombs", "Saadian Tombs", "Tombe saadiane", 31.6173, -7.9886,
             note_en="Ahmad al-Mansur, the most powerful of the Saadian sultans, lies at the centre of the Chamber of the Twelve Columns, among his dynasty's tombs.",
             note_it="Ahmad al-Mansur, il più potente dei sultani saadiani, riposa al centro della Sala delle dodici colonne, tra le tombe della sua dinastia."),
        # Source: Wikipedia, Saadian Tombs; Wikipedia (fr), Tombeaux saadiens.
        Stop("marrakech_el_badi", "El Badi Palace", "Palazzo El Badi", 31.6182, -7.9865,
             note_en="Ahmad al-Mansur began the Incomparable Palace in 1578, after the battle of the Three Kings; Moulay Ismail later stripped it to build Meknes.",
             note_it="Ahmad al-Mansur iniziò il Palazzo Incomparabile nel 1578, dopo la battaglia dei Tre Re; Moulay Ismail in seguito lo spogliò per costruire Meknès."),
        # Source: Wikipedia, El Badi Palace; Wikipedia (fr), Palais El Badi. When it was stripped differs (1696 or 1707): not said.
        Stop("marrakech_mellah", "Mellah", "Mellah", 31.6198, -7.9849,
             note_en="The Saadian sultan Abdallah al-Ghalib created this Jewish quarter by decree in 1558, beside the royal kasbah.",
             note_it="Il sultano saadiano Abdallah al-Ghalib creò per decreto questo quartiere ebraico nel 1558, accanto alla kasbah reale."),
        # Source: Wikipedia, Mellah of Marrakesh; Wikipedia (fr), Mellah de Marrakech. The stop is the Place des Ferblantiers, at its edge.
        Stop("marrakech_bahia", "Bahia Palace", "Palazzo della Bahia", 31.6219, -7.9822,
             note_en="Begun in the 1860s by the grand vizier Si Musa, the palace was enlarged until 1900 by his son Ba Ahmed, grand vizier after him.",
             note_it="Iniziato negli anni Sessanta dell'Ottocento dal gran visir Si Musa, il palazzo fu ingrandito fino al 1900 da suo figlio Ba Ahmed, gran visir dopo di lui."),
        # Source: Wikipedia, Bahia Palace; Wikipedia (fr), Palais de la Bahia. Where its name comes from is in one only: not said.
        Stop("marrakech_dar_si_said", "Dar Si Said", "Dar Si Said", 31.6234, -7.9839,
             note_en="Built for Si Said, Ba Ahmed's brother, the house became a museum of Moroccan crafts in the 1930s.",
             note_it="Costruita per Si Said, fratello di Ba Ahmed, la casa divenne negli anni Trenta un museo dell'artigianato marocchino."),
        # Source: Wikipedia, Dar Si Said (1930 or 1932); Wikipedia (fr), Musée Dar Si Saïd (1932).
    ],
)

FEZ = Walk(
    id="FEZ_PALACE_ANDALUSIANS",
    city="fez",
    city_en="Fez",
    city_it="Fès",
    route_en="From the Royal Palace to the Andalusian Mosque, by Bou Inania, al-Qarawiyyin and the tanneries",
    route_it="Dal Palazzo Reale alla Moschea degli Andalusi, passando per Bou Inania, al-Qarawiyyin e le concerie",
    outing_en="A walk in Fez",
    outing_it="Passeggiata a Fès",
    country="MA",
    continent="AFRICA",
    # The medina's lanes are mapped as residential streets.
    more_streets=("residential", "unclassified", "living_street"),
    more_streets_min_metres=200,
    # The Oued Fès and the Oued Jawahir by Jnan Sbil; the Oued Boukhrareb, through the old city.
    water=["way/1271557555", "way/1271557562"],
    canals=["way/160083098", "way/486917300", "way/121766257"],
    # Jnan Sbil, the Dar el-Beida's garden.
    parks=["way/95233178", "way/1496105508"],
    stops=[
        Stop("fez_royal_palace", "Royal Palace", "Palazzo Reale", 34.0530, -4.9928,
             note_en="The royal palace covers 80 hectares; its great ornate doors on the Place des Alaouites were made in the 20th century.",
             note_it="Il palazzo reale si estende su 80 ettari; le sue grandi porte decorate su Place des Alaouites furono realizzate nel Novecento."),
        # Source: Wikipedia, Royal Palace of Fez (1969 to 1971); Wikipedia (fr), Palais royal (Fès) (their designer honoured about 1972).
        Stop("fez_mellah", "Mellah", "Mellah", 34.0533, -4.9908,
             note_en="Moved here under the Marinids, Fez's Jewish quarter was the first in Morocco to be called a mellah, perhaps after a salt store or a salty spring.",
             note_it="Trasferito qui sotto i Merinidi, il quartiere ebraico di Fès fu il primo del Marocco a chiamarsi mellah, forse per un magazzino di sale o una fonte salata."),
        # Source: Wikipedia, Mellah of Fez; Wikipedia (it), Mellah di Fès.
        Stop("fez_jnan_sbil", "Jnan Sbil Gardens", "Giardini Jnan Sbil", 34.0599, -4.9881,
             note_en="Laid out by Sultan Hassan I between Fez's two old cities, the gardens were reserved for the royal elite until 1917.",
             note_it="Creati dal sultano Hassan I tra le due città vecchie di Fès, i giardini furono riservati all'élite reale fino al 1917."),
        # Source: Wikipedia, Jnan Sbil Gardens; Wikipedia (es), Jardines Jnan Sbil.
        Stop("fez_bou_inania", "Bou Inania Madrasa", "Madrasa Bou Inania", 34.0623, -4.9827,
             note_en="Built from 1350 to 1355 for the Marinid sultan Abu Inan, the madrasa was also a Friday mosque, with its own minaret; a water clock stands beside it.",
             note_it="Costruita dal 1350 al 1355 per il sultano merinide Abu Inan, la madrasa era anche moschea del venerdì, con un suo minareto; accanto c'è un orologio ad acqua."),
        # Source: Wikipedia, Bou Inania Madrasa (the clock across the street); Wikipedia (fr), Médersa Bou Inania de Fès (on its façade): the sentence says beside it. Bab Bou Jeloud, just before, has no place of its own.
        Stop("fez_moulay_idriss", "Zawiya of Moulay Idris II", "Zawiya di Moulay Idris II", 34.0648, -4.9747,
             note_en="The shrine holds the tomb of the Idrisid ruler Idris II, found again around 1437; Moulay Ismail later gave it its pyramidal roof.",
             note_it="Il santuario custodisce la tomba del sovrano idriside Idris II, ritrovata intorno al 1437; Moulay Ismail le diede poi il tetto piramidale."),
        # Source: Wikipedia, Zawiya of Moulay Idris II; Wikipedia (fr), Mausolée de Moulay Idriss II. Whether he founded the city is in one only: not said.
        Stop("fez_qarawiyyin", "Al-Qarawiyyin", "Al-Qarawiyyin", 34.0648, -4.9733,
             note_en="Founded as a mosque by Fatima al-Fihri, of a family from Kairouan, al-Qarawiyyin is cited by UNESCO as the oldest university still at work.",
             note_it="Fondata come moschea da Fatima al-Fihri, di una famiglia di Kairouan, al-Qarawiyyin è citata dall'UNESCO come la più antica università ancora attiva."),
        # Source: Wikipedia, University of al-Qarawiyyin; Wikipedia (fr), Université Al Quaraouiyine. Its year differs (857 or 859, about 859): not said.
        Stop("fez_chouara", "Chouara Tannery", "Conceria Chouara", 34.0659, -4.9710,
             note_en="Hides are softened in the white vats and dyed in the coloured ones; local tradition dates the tannery back to the founding of Fez.",
             note_it="Le pelli vengono ammorbidite nelle vasche bianche e tinte in quelle colorate; la tradizione locale fa risalire la conceria alla fondazione di Fès."),
        # Source: Wikipedia, Chouara Tannery; Wikipedia (de), Chouara-Gerberei.
        Stop("fez_andalusian_mosque", "Andalusian Mosque", "Moschea degli Andalusi", 34.0632, -4.9681,
             note_en="Founded in 859 and 860 by Maryam al-Fihri, Fatima's sister, the mosque is known for its tall north gate of zellij and carved wood.",
             note_it="Fondata nell'859 e 860 da Maryam al-Fihri, sorella di Fatima, la moschea è nota per la sua alta porta nord di zellige e legno scolpito."),
        # Source: Wikipedia, Mosque of the Andalusians; Wikipedia (fr), Mosquée des Andalous.
    ],
)

WALKS = [MILAN, ROME, PARIS, LONDON, MADRID, BERLIN, VIENNA, PORTO, AMSTERDAM, PRAGUE, LIMA, CUSCO, NEW_YORK, RIO, MEXICO_CITY, BUENOS_AIRES, SAN_FRANCISCO, QUEBEC, HAVANA, CARTAGENA, TOKYO, SYDNEY, SEOUL, BEIJING, HONG_KONG, SINGAPORE, BANGKOK, KYOTO, HANOI, MELBOURNE, CAIRO, CAPE_TOWN, MARRAKECH, FEZ]

# The locator map's frame for each country (south, west, north, east), in degrees.
LOCATORS = {
    "IT": (36.3, 6.3, 47.4, 18.8),
    # Spain and Portugal, one peninsula: the two Caminos to Santiago.
    "IBERIA": (35.8, -9.7, 44.0, 3.6),
    "GB": (49.8, -8.4, 59.0, 2.2),
    "FR": (41.3, -5.2, 51.1, 9.6),
    "PE": (-18.6, -81.6, 0.2, -68.4),
    "NL": (50.7, 3.3, 53.6, 7.3),
    "CZ": (48.5, 12.0, 51.1, 18.9),
    "DE": (47.2, 5.8, 55.1, 15.1),
    "AT": (46.3, 9.5, 49.1, 17.2),
    "US": (24.5, -125.0, 49.5, -66.9),
    "BR": (-33.8, -74.0, 5.3, -34.8),
    "MX": (14.5, -118.4, 32.7, -86.7),
    "AR": (-55.1, -73.6, -21.8, -53.6),
    "CA": (41.6, -141.0, 70.0, -52.6),
    "CU": (19.8, -85.0, 23.3, -74.1),
    "CO": (-4.3, -79.1, 12.5, -66.8),
    "JP": (24.0, 122.9, 45.6, 146.0),
    "AU": (-43.7, 112.9, -10.6, 153.7),
    "KR": (33.1, 124.6, 38.6, 131.9),
    "CN": (18.2, 73.5, 53.6, 134.8),
    "HK": (22.15, 113.83, 22.57, 114.44),
    "SG": (1.16, 103.6, 1.48, 104.1),
    "TH": (5.6, 97.3, 20.5, 105.7),
    "VN": (8.4, 102.1, 23.4, 109.5),
    "EG": (21.7, 24.7, 31.7, 36.9),
    "ZA": (-34.9, 16.4, -22.1, 32.9),
    "MA": (27.6, -13.3, 36.0, -1.0),
}

# The continents the Ways page groups the cities by, in its order (the Kotlin enum Continent has
# the same names): the frame of each one's map (south, west, north, east), in degrees, drawn
# from Natural Earth's land. A continent is listed only once it has a walk: the build fails on
# one without, and on a walk whose route falls outside its continent's frame.
CONTINENTS = {
    # From Lisbon to Istanbul and Helsinki: the cities a walk is likely to visit, not the Urals.
    "EUROPE": (34.5, -11.5, 61.5, 31.5),
    # From southern Canada to Cape Horn, the Pacific coast to Brazil's eastern tip.
    "AMERICAS": (-56.0, -126.0, 56.0, -33.0),
    # From Mumbai and Delhi to Japan, and down to Tasmania and New Zealand: the cities a walk is
    # likely to visit, not the Middle East (Istanbul is Europe's).
    "ASIA_OCEANIA": (-47.5, 66.0, 46.0, 179.0),
    # The whole continent, from Dakar to the Horn of Africa and from Tunis to the Cape.
    "AFRICA": (-35.5, -18.5, 38.0, 52.0),
}
