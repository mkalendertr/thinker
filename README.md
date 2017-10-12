# THINKER - Entity Linking System for Turkish Language (Türkçe Anlamsal Varlık Bulma Sistemi)

THINKER is a novel entity linking system (semantic tagging or annotation) for Turkish language that automatically maps entity mentions in  a  text  content  to  the  corresponding  real  world  entities defined in Vikipedi or the Turkish dictionary published by Turkish  Language  Association  (TLA). 

A simple demo of the system is running at this
[address](http://ec2-54-245-18-29.us-west-2.compute.amazonaws.com/).
The REST-api is available at:
http://ec2-54-245-18-29.us-west-2.compute.amazonaws.com/api/rest/.
Sample linking request -
http://ec2-54-245-18-29.us-west-2.compute.amazonaws.com/api/rest/annotate?myspotter=false&text=Yeditepe%20%C3%9Cniversitesi%20bir%20vak%C4%B1f%20universitedir.

You can also download the binary distribution from this [address](https://drive.google.com/file/d/0ByNllFoe5H6ZenZnRFJuaXdGVVk/view?usp=sharing).

## Citation

If you use the Thinker framework, you must cite:

> M. Kalender and E. E. Korkmaz.
> THINKER - Entity Linking System for Turkish Language.
> *IEEE Transactions on Knowledge and Data Engineering, vol. PP, no. 99, pp. 1-1.* 
doi: 10.1109/TKDE.2017.2761743


Bibtex format:

	@inproceedings{DBLP:conf/cikm/CeccarelliLOPT13a,
	  author    = {Diego Ceccarelli and
	               Claudio Lucchese and
	               Salvatore Orlando and
	               Raffaele Perego and
	               Salvatore Trani},
	  title     = {Dexter: an open source framework for entity linking},
	  booktitle = {ESAIR'13, Proceedings of the Sixth International Workshop on Exploiting
	               Semantic Annotations in Information Retrieval, co-located with {CIKM}
	               2013, San Francisco, CA, USA, October 28, 2013},
	  pages     = {17--20},
	  year      = {2013},
	  crossref  = {DBLP:conf/cikm/2013esair},
	  url       = {http://doi.acm.org/10.1145/2513204.2513212},
	  doi       = {10.1145/2513204.2513212},
	  timestamp = {Thu, 15 May 2014 15:51:38 +0200},
	  biburl    = {http://dblp.uni-trier.de/rec/bib/conf/cikm/CeccarelliLOPT13a},
	  bibsource = {dblp computer science bibliography, http://dblp.org}
	}

## Experimental Datasets
Besides source codes of the project, we publish taxonomy represented for entity discovery, pre-trained entity vectors and experiment data sets:
  - [Turkish_Entity_Linking_Dataset](https://drive.google.com/file/d/0B44czttSzeAAWjNJbGpNVnd6Ukc3aklFNzFfS3BCY3BYSENF/view?usp=sharing); The dataset contains approximately 5 news articles for 112 different meanings from online news papers covering a variety of Turkish ambiguous phases such as pas, petrol, etc.. 
  - [Vikipedi_Entity_Vectors](https://drive.google.com/file/d/0B44czttSzeAATWt6VTB0eVdjU2s/view?usp=sharing): The archive contains two separate json files. Metadata Vectors: Entity vectors trained on category, type and infobox information of Vikipedi articles. The model is distributed in 300-dimensional vectors. Link Vectors: Entity vectors trained on link information of Vikipedi articles. The model is distributed in 150-dimensional vectors.
  - The taxonomy used in the entity discovery process and WordNet mappings are provided in the table below:

| Parent Type | Type | WordNet 3.1 Mapping |
| ------ | ------ | ------ |
|animal |  kuş |  bird#1
|animal |  bitki |  plant#2
|animal |  örümcek |  spider#1
|animal |  memeli |  mammal#1
|animal |  balık |  fish#1
|animal |  böcek |  insect#1 
|animal |  sürüngen |  reptile#1
|animal |  köpek ırkı |  dog#1
|animal |  yılan |  snake#1
|animal |  primat |  primate#1
|animal |  bakteri |  bacteria#1
|animal |  biyoloji |  biology#1
|concept |  etnik grup |  ethnic group#1
|concept |  dil |  language#1
|concept |  kraliyet |  kingdom#1
|concept |  hastalık |  disease#1
|concept |  bayrak |  flag#1
|concept |  mitoloji |  mythology#1
|concept |  marş |  anthem#1
|concept |  dil ailesi |  language#1
|concept |  anatomi |  anatomy#2
|concept |  ödül |  award#3
|concept |  islam |  islam#1
|concept |  harf |  letter#2
|concept |  programlama dili |  programming language#1
|concept |  renk |  color#1
|concept |  kimya |  chemistry#1
|concept |  sayı |  number#5
|concept |  fizik |  physics#2
|concept |  hazır gıda |  food#2
|concept |  matematik |  mathematics#1
|creative work |  film |  movie#1
|creative work |  albüm |  album#1
|creative work |  tekli |  single#2
|creative work |  kitap |  book#1
|creative work |  televizyon |  television#1
|creative work |  şarkı |  song#1
|creative work |  yazılım |  software#1
|creative work |  video oyunu |  video game#1
|creative work |  tv bölüm |  episode#1
|creative work |  opera |  opera#1
|creative work |  oyun |  game#1
|creative work |  doctor who bölüm |  episode#1
|creative work |  single |  song#1
|creative work |  işletim sistemi |  operating system#1
|creative work |  sure |  prayer#4
|creative work |  televizyon sezonu |  season#1
|creative work |  müzik türü |  music genre#1
|creative work |  öykü |  story#1
|creative work |  dizi |  episode#1
|creative work |  prison break bölümü |  episode#1
|creative work |  tiyatro oyunu |  play#1
|creative work |  sanatçı diskografisi |  discography#1
|creative work |  roman |  novel#1
|creative work |  simpsonlar bölümü |  episode#1
|creative work |  sanat eseri |  artwork#1
|creative work |  edebiyat |  literature#1 
|creative work |  müzik |  music#1
|creative work |  lost |  serial#1
|creative work |  lost karakteri |  
|creative work |  seri |  serial#1
|event |  savaş |  war#1
|event |  futbol maçı |  match#2
|event |  futbol kulübü sezonu |  season#1
|event |  uluslararası futbol turnuvası |  tournament#1
|event |  futbol ligi sezonu |  season#1
|event |  futbol turnuvası |  tournament#1
|event |  antlaşma |  agreement#1
|event |  sivillere karşı saldırı |  attack#1
|event |  seçim |  election#1
|event |  konser turnesi |  tour#1
|event |  eurovision |  contest#1
|event |  deprem |  earthquake#1
|event |  nba draft |  
|event |  olimpiyatlar |  Olympics#1
|event |  bayram |  holiday#1
|event |  parlamento |  parliament#1
|event |  spor derbisi |  
|event |  olimpiyatlarda etkinlik |  Olympics#1
|event |  spor ligi |  league#1
|event |  festival |  festival#1
|event |  kral tv vmö |  award#2
|location |  yerleşim |  placement#3
|location |  türkiye köy |  village#1
|location |  portekiz bucak |  district#1
|location |  almanya yerleşim yeri |  placement#3
|location |  türkiye ilçe |  county#1 
|location |  eski ülke |  country#1
|location |  portekiz belediye |  municipality#1 
|location |  ülke |  county#1 
|location |  nehir |  river#1
|location |  göl |  lake#1
|location |  eski idari bölüm |  
|location |  italya il |  city#1
|location |  ada |  island#1
|location |  türkiye belde |  town#1
|location |  iran  köy |  village#1
|location |  italya belde |  town#1
|location |  koruma alanı |  
|location |  dağ |  mountain#1
|location |  türkiye il |  city#1
|location |  abd eyalet |  state#1
|location |  il |  city#1
|location |  şehir |  city#1
|location |  türkiye mahalle |  vicinity#1
|location |  cadde |  avenue#2
|location |  italya komün |  commune#1
|location |  uydu |  satellite#1
|location |  türkiye |  Turkey#2
|location |  ilçe |  district#1
|location |  bulgaristan il |  city#1
|location |  çin eyalet |  state#1
|location |  ukrayna idari birimi |  administrative unit#1
|location |  kanton |  canton#2
|object |  otomobil |  automobile#1
|object |  ateşli silah |  firearm#1
|object |  uçak |  airplane#1
|object |  gemi |  ship#1
|object |  arma |  rigging#1
|object |  cep telefonu |  cell phone#1 
|object |  amfibi |  amphibian#3
|object |  para |  money#1
|object |  uzay aracı |  spacecraft#1
|organization |  futbol kulübü |  football team#1
|organization |  şirket |  company#1
|organization |  tv kanalı |  channel#7
|organization |  üniversite |  university#1
|organization |  kuruluş |  establishment#1
|organization |  siyasi parti |  political party#1
|organization |  gazete |  newspaper#1
|organization |  dergi |  magazine#1
|organization |  millî futbol takımı |  
|organization |  website |  website#1
|organization |  basketbol kulübü |  basketball team#1
|organization |  futbol ligi |  football league#1
|organization |  radyo istasyonu |  radio station#1
|organization |  milli futbol cemiyeti |  association#1 
|organization |  rusya federasyonu idari birimi |  administrative unit#1
|organization |  silahlı kuvvet |  armed forces#1
|organization |  havayolları |  airline#2
|organization |  hükümet kurumu |  institution#1
|organization |  basketbol ligi |  basketball league#1
|organization |  voleybol kulübü |  team#1
|organization |  azerbaycan rayon |  
|organization |  siyasi makam |  agency#1
|organization |  nba takımı |  team#1
|organization |  plak şirketi |  record company#1
|organization |  bm |  
|organization |  telekomünikasyon şirketi |  company#1
|organization |  basketbol |  basketball#1
|organization |  sivil toplum kuruluşu |  nongovernmental organization#1
|person |  futbolcu |  football player#1
|person |  müzik sanatçısı |  musician#2
|person |  kişi |  person#1
|person |  oyuncu |  actor#1
|person |  makam sahibi |  authority#2
|person |  yazar |  writer#1
|person |  bilim adamı |  scientist#1
|person |  basketbolcu |  basketball player#1
|person |  asker |  soldier#1
|person |  hükümdar |  monarch#1
|person |  sanatçı |  artist#1
|person |  porno yıldızı |  
|person |  sporcu |  athlete#1
|person |  filozof |  philosopher#1
|person |  kurgusal karakter |  
|person |  tenis sporcu |  
|person |  çizgi roman karakteri |  fictional character#1
|person |  güreşçi |  wrestler#1
|person |  manken |  model#8
|person |  sunucu |  presenter#1
|person |  buz patencisi |  skater#1
|person |  voleybolcu |  volleyball player#1
|person |  karakter |  fictional character#1
|person |  hakem |  referee#1
|person |  yıldız savaşları karakteri |  fictional character#1
|person |  mimar |  architect#1
|person |  bisikletçi |  cyclist#1
|person |  doctor who karakteri |  fictional character#1
|person |  müzik grubu |  band#2
|person |  harry potter karakteri |  fictional character#1
|person |  avrupa yakası karakteri |  fictional character#1
|person |  hanedan |  dynasty#1
|person |  insan |  human#1 
|physical object |  galaksi |  galaxy#3
|physical object |  takımyıldız |  constellation#2
|physical object |  güneşdışı gezegen |  planet#1
|structure |  stadyum |  stadium#1
|structure |  havalimanı |  airport#1
|structure |  dini yapı |  place of worship#1
|structure |  askeri birim |  military unit#1
|structure |  baraj |  dam#1
|structure |  yapı |  structure#1
|structure |  okul |  school#1
|structure |  müze |  museum#1
|structure |  yüksek yapı |  structure#1
|structure |  yol |  road#1
|structure |  fakülte |  faculty#2
|structure |  köprü |  bridge#1
|structure |  askeri yapı |  structure#1
