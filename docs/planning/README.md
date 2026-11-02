# PLANNING's archive

The closed parts of Passo's plan, one per file, **frozen**: never edited. The live plan (how it
is made, the state, what is open, the architecture, the phases not done, the current log) is
[`PLANNING.md`](../../PLANNING.md), which also says how the plan is archived. A decision that
changes one of these is written there, naming the one it changes.

This index is written by `tools/planning_index.py`: never edited by hand. Every title (`##`,
`###`, `####`) and every log entry (by its bold title) of the archive, with its line, to find a
reference by name and read from the right place. The current log is not here: it is in
`PLANNING.md`, short, and searched there.


## [01-phases-0-14.md](01-phases-0-14.md): PLANNING, archive 01: the phases done, 0 to 14 (to 2 Nov 2026)

- L12: 11. Delivery phases
  - L17: Phase 0 — Foundations
  - L39: Phase 1 — Tracking engine (highest risk)
  - L62: Phase 2 — Profile, settings, metrics
  - L78: Phase 3 — Today screen and onboarding
  - L108: Phase 4 — Widget
  - L135: Phase 5 — History and insights
  - L165: Phase 6 — Goals and system surfaces
  - L189: Phase 7 — Data, calibration and polish
  - L215: Phase 8 — Release on GitHub
  - L237: Phase 10 — Outings (walks with a goal)
  - L274: After v1.0
  - L283: Phase 11 — The Ways («I Cammini»)
    - L344: Phase 11, part two — City walks («Passeggiate in città»)
    - L389: Phase 11, later — The fifth way, and the other cities
  - L585: Phase 13 — Interval walk («Camminata a intervalli»)
  - L673: Phase 14 — More ways, beyond Europe («Altri cammini»)

## [02-decisions-to-1-nov-2026.md](02-decisions-to-1-nov-2026.md): PLANNING, archive 02: the decisions log, §15, to 1 Nov 2026

- L11: 15. Decisions log: decided
  - L28: Phase 1: no diagnostics screen and no wake-up vs non-wake-up experiment
  - L29: Start from boot, from the documentation
  - L36: Phase 2: summaries, the profile and the frozen past
  - L39: Energy cost by cadence:
  - L40: Profile limits:
  - L41: Sex
  - L42: Units:
  - L43: Formatting:
  - L46: Phase 3: Chiaro's design language
  - L48: Live count while visible
  - L49: Pause means "don't count these"
  - L51: Brisk minutes are shown against the day's share of the WHO's 150 a week
  - L54: README screenshots
  - L55: Phase 4: two widgets in Chiaro's dress
  - L56: Widget progress ring: a bitmap
  - L57: Widget repaints are pushed and decided by a pure policy
  - L58: The widget's count is Today's
  - L59: A count that is not moving says so on the card
  - L61: PassoColors.attention
  - L62: Glance 1.2.0 on WorkManager 2.10.5
  - L63: A widget never waits forever
  - L65: Phase 5: History, Insights and walks
  - L66: One bar chart for hours, days and months
  - L67: The bottom bar
  - L68: Walk types
  - L69: Backup, decided
  - L70: Walk detection thresholds kept
  - L71: Battery tip: no Samsung, no external guide
  - L72: The counting notification: no in-app switch
  - L73: The launcher icon: Chiaro's ring, Passo's step
  - L74: Phase 6: goals and system surfaces
  - L75: Evening reminder alarm: setAndAllowWhileIdle with a wakeup
  - L76: Evening reminder threshold
  - L77: The reminder reads the count after a flush
  - L78: The weekly summary
  - L79: The Quick Settings tile
  - L82: Phase 10: Outings
  - L83: Outings: one quantity per goal
  - L84: Outings: the wake-up step counter during an outing only
  - L85: Outings: time is time in motion
  - L86: Outings: vibrations in counts
  - L87: Outings: the counting notification becomes the outing's
  - L88: Outings: the notification permission is asked in context
  - L89: Outings: an outing replaces the walk found in its minutes
  - L90: Outings on the widgets and the tile
  - L92: Outings: the voice's tone and variety
  - L93: Outings: the voice
  - L95: Phase 7: export, import and calibration
  - L96: The backup format is kotlinx.serialization in :core:domain
  - L97: Import merges by the larger count per minute, never the sum
  - L98: CSV: fixed English headers with units in their names, RFC 4180, UTF-8 with a BOM
  - L99: The calibration does not use the tracking service
  - L100: §3 reviewed at Phase 7
  - L102: Today's "Start an outing" is the reader's to take away
  - L104: The guide, in Chiaro's shape
  - L106: The real release key, and the temporary one retired
  - L108: Outings: an end by stillness is told, and can be taken back
  - L109: The outing editor with the keyboard up
  - L111: The shell on Chiaro's back stacks
  - L113: No tablet layout; foldables as one centred column
  - L114: The accessibility pass is automated where it can be
  - L115: Today's chart across the whole card, and 12 more dp above the fold
  - L116: The README in the family's shared structure
  - L117: v1.0.0
  - L118: The release page is the CHANGELOG section alone
  - L120: Route maps with GPS stay out
  - L121: After v1.0: three phases, in the owner's order
  - L122: The peak cadence and the weekly rhythm dropped as features
  - L124: The Ways: OpenStreetMap lines, four ways, the Francigena's Italian part
  - L125: The interval walk: ADR 0013 accepted
  - L126: The interval walk, built
  - L127: Steps laid over the time they took, at every gap
  - L128: An outing's cadence over the time in motion its steps took
  - L129: Signals on time on phones a step counter cannot wake: the processor kept awake
  - L130: A signal hidden by a phone call is said when the call ends; the log kept small
  - L132: City walks, as Phase 11's second part
  - L134: Your year: one card on Today in December
  - L136: The Ways, first part
  - L137: City walks, built
  - L138: Three refinements after trying the walks
  - L140: The Camino named as readers know it
  - L142: Five ways and five cities, then stop
  - L144: No limit on ways and cities; one walk a city for now
  - L145: The fifth way and the other cities, built
  - L146: The main streets on a city's map
  - L147: Milan's and Rome's maps as full as the others'
  - L148: The locator keeps clear of the ends' names
  - L149: Two lengths of city walk: about 5 km and about 10 km
  - L150: A city walk begun can be left
  - L151: Porto, Amsterdam and Prague, the first short walks
  - L153: Nothing on a card is cut
  - L154: The cities by continent
  - L155: Berlin and Vienna, added without moving the other cities
  - L156: The coast from OpenStreetMap, not Natural Earth
  - L157: Mexico City and Buenos Aires
  - L158: San Francisco, Québec, Havana and Cartagena
  - L159: How a city is added, written down
  - L160: Asia and Oceania, with Tokyo and Sydney
  - L162: Seoul and Beijing
  - L164: Hong Kong and Singapore
  - L166: Bangkok and Kyoto
  - L168: Hanoi and Melbourne, Asia and Oceania's ten
  - L170: Hanoi's bridge and Melbourne's gardens, in more detail
  - L171: Africa, with Cairo and Cape Town
  - L172: Marrakech and Fez
  - L173: Cape Town's hill and streets
  - L174: Tunis and Alexandria
  - L175: Dakar and Addis Ababa
  - L176: Zanzibar and Luxor, Africa's ten
  - L177: The ways' maps, redrawn with the cities' detail
  - L179: A sentence for every stage that has one, starting with the Via Francigena
  - L181: The Camino Francés's stages
  - L183: The Via di Francesco's stages
  - L185: The Via degli Dei's and the Camino Portugués's stages
  - L187: A removed card no longer crashes the process
  - L189: The sister apps stay out of the app and the code
  - L191: The widgets' type scale
  - L193: The launcher's corner
  - L195: The words card's sentence on the scale
  - L197: Five of the six stages left with a name
  - L199: Today, after its review
  - L200: Today's review, checked on screenshots
  - L202: The widget settings, reset
  - L204: Today's chart whole on the first screen
  - L206: An outing's start is seen where it was touched
  - L208: More ways, beyond Europe
  - L210: Phase 14's words, stamps and groups
  - L212: The Kumano Kodō and the Great Ocean Walk
  - L233: The Inca Trail and the John Muir Trail
  - L250: Kilimanjaro, and the phase's end
  - L270: The two Caminos named alike, and each list of the Ways page introduced
  - L272: Phase 11's device checks done, and the Ways reviewed
  - L274: The way under way on Today
  - L275: A distance in the Ways' texts never breaks from its unit
  - L276: Three more breaks at a large text size, mended without moving a short line
  - L277: Saint-Jean-Pied-de-Port's sentence
  - L279: The family's icons, made one suite
  - L281: The family's onboarding frame
  - L283: 18 Oct 2026. Time zones and clock changes, reviewed

## [03-towards-1.1.0.md](03-towards-1.1.0.md): PLANNING, archive 03: towards 1.1.0, 2 Nov 2026

- L12: 11. Delivery phases: Phase 12
  - L14: Phase 12 — Your year on foot («Il tuo anno a piedi»)
- L47: 15. Decisions log: 2 Nov 2026
  - L49: 2 Nov 2026. A review of the wakes, the notifications and the battery
  - L56: Insights' streak card says since when; the interval walk has a mark of its own
  - L57: A crash page in test builds
  - L58: The plan archived as the family's
  - L60: Your year on foot
  - L73: The last seven days on At a glance
  - L75: 1.1.0, prepared
