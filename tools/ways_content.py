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
and Buenos Aires in the English and Spanish.
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

WALKS = [MILAN, ROME, PARIS, LONDON, MADRID, BERLIN, VIENNA, PORTO, AMSTERDAM, PRAGUE, LIMA, CUSCO, NEW_YORK, RIO, MEXICO_CITY, BUENOS_AIRES]

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
}
