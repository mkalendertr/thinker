# THINKER - Entity Linking System for Turkish Language

THINKER is a novel entity linking system for Turkish language that automatically maps entity mentions in  a  text  content  to  the  corresponding  real  world  entities defined in Vikipedi or the Turkish dictionary published by Turkish  Language  Association  (TLA). 

Besides source codes of the project, we publish taxonomy represented for entity discovery, pre-trained entity vectors and experiment data sets:
  - [Turkish_Entity_Linking_Dataset](https://drive.google.com/file/d/0B44czttSzeAAWjNJbGpNVnd6Ukc3aklFNzFfS3BCY3BYSENF/view?usp=sharing); The dataset contains approximately 5 news articles for 112 different meanings from online news papers covering a variety of Turkish ambiguous phases such as pas, petrol, etc.. 
  - [Vikipedi_Entity_Vectors](https://drive.google.com/file/d/0B44czttSzeAATWt6VTB0eVdjU2s/view?usp=sharing): The archive contains two separate json files. Metadata Vectors: Entity vectors trained on category, type and infobox information of Vikipedi articles. The model is distributed in 300-dimensional vectors. Link Vectors: Entity vectors trained on link information of Vikipedi articles. The model is distributed in 150-dimensional vectors.
  - The taxonomy used in the entity discovery process and WordNet mappings are provided in the table below:

| Parent Type | Type | WordNet 3.1 Mapping |
| ------ | ------ | ------ |
| animal | kuş | bird#1
| Github | [plugins/github/README.md] [PlGh] |
| Google Drive | [plugins/googledrive/README.md] [PlGd] |
| OneDrive | [plugins/onedrive/README.md] [PlOd] |
| Medium | [plugins/medium/README.md] [PlMe] |
| Google Analytics | [plugins/googleanalytics/README.md] [PlGa] |
