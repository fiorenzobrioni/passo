"""The Ways' content: which OpenStreetMap relations draw each way, its stops, and what is said
about them, in English and Italian. Read by tools/build_ways.py, which writes the app's data
from it; this file is the one to edit.

A stop is a stage (a stamp in the credential, a notification when reached) or, with
stage=False, a place on the way worth a line (the sanctuary of San Luca, the Cisa Pass). Its
coordinates only find it on the line: the script snaps each one to the nearest point of the
way and measures its distance from there, and fails if a stop is more than MAX_SNAP_METRES off
the line or out of order.

Every note is one sentence, made to be read in a notification and heard, with nothing that
goes stale (no opening hours, no prices). Each was checked when written (1 Oct 2026); the
source is the line after it.
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
    country: str  # the locator map's country: IT or ES
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
    country="ES",
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

WAYS = [VIA_DEGLI_DEI, VIA_DI_FRANCESCO, CAMINO_FRANCES, VIA_FRANCIGENA]


@dataclass(frozen=True)
class Walk:
    """A walk through a city (Phase 11's second part), walked as an outing.

    Its line is routed once by BRouter (a walking router on OpenStreetMap data) through its
    places, in order, and saved in tools/walks/ by the script's `fetch`: the places are the
    route's waypoints, so each lies on the line. Behind it, the city's water and largest parks,
    from OpenStreetMap objects named here by id ("relation/28934"); canals are lines drawn
    [canal_width] metres wide.
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
    stops: list = field(default_factory=list)
    water: list = field(default_factory=list)
    canals: list = field(default_factory=list)
    parks: list = field(default_factory=list)
    canal_width: float = 20.0


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
    water=["way/345030396"],
    canals=["relation/3340142", "relation/3350738"],
    parks=["relation/10172760", "way/5110320", "way/260829251"],
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

WALKS = [MILAN, LONDON]

# The locator map's frame for each country (south, west, north, east), in degrees.
LOCATORS = {
    "IT": (36.3, 6.3, 47.4, 18.8),
    "ES": (35.8, -9.7, 44.0, 3.6),
    "GB": (49.8, -8.4, 59.0, 2.2),
}
