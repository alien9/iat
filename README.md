IAT

# Investigação de Acidentes de Trânsito

Esta aplicação cria registros de incidentes de trânsito. O IAT pode ser acionado por uma outra aplicação no ambiente Android. Uma implementação possível é por meio do plugin *bigrs-croqui*, que é compatível com o ambiente Cordova.
Tanto o plugin quanto qualquer outra aplicação Android acionam o IAT por meio do Intent action br.com.cetsp.iat.DRAW.
A aplicação está preparada para ser receber alguns parâmetros e responder para o emissor com os dados produzidos, que incluem os veículos envolvidos, suas posições na cena e o croqui detalhando a ocorrência.

**Ambiente**

A versão atual foi compilada com o Android SDK 26. 

**Parâmetros de entrada**

A aplicação é parametrizada com uma string extra com a chave _info_. Este dataset pode conter a posição atual (dada peo GPS, por exepmplo), bem como as instruções para carga de um relatório existente, que assim pode ser editado pela aplicação.

Lista de parâmetros (opcionais):

* placas: PLacas de veículos envolvidos, concatenadas por vírgulas

* info: relatório existente a ser editado

* latitude: latitude

* longitude: longitude

* zoom [20]: Nível inicial de zoom

* size [300]: largura em pixels da imagem do croqui a ser anexada ao relatório


**Configurações**

A aplicação renderiza mapas de acordo com a localização fornecida na inicialização ou a partir da leitura de dados de GPS.
Estão configurados dois layers de renderização de mapas: o layer básico com contornos de quadras e o de rótulos de logradouros.  
O layer de desenho de quadras é definido pela tabela _quadras_ com a geometria configurada com o sistema de coordenadas WGS84 (EPSG 4326), assim como o de logradouros com a tabela _logradouros_. O campo _nome_ na tabela logradouros determina qual será o campo usado para rotular os locais.

O banco de dados pode ser gerado a partir dos arquivos ESRI Shapefile correspondentes ao mapa de quadras e logradouros. Para tanto pode-se empregar a aplicação [spatialite-gui](http://www.gaia-gis.it/gaia-sins/index.html) que permite importar os dados a partir dos arquivos ESRI Shapefile do mapa da cidade. O banco de dados faz oarte da compilação do aplicativo. Para alterar a versão dos mapas disponíveis no aparelho é preciso substituir o arquivo correspondente à base de dados em app/src/main/res/raw/db. 


**Formato de Dados**
Os relatórios produzidos com a ferramenta IAT Android são retornados para a aplicação acionadora serializados - isto é, o formato é texto JSON. Cada registro consiste em um conjunto de informações que descrevem o incidente e permitem reconstituir a cena retratada no relatório. Todas as imagens são serializadas com o padrão base64.

No primeiro nível da estrutura temos os parâmetros:

**image**: em formato PNG, com as dimensões especificadas na chamada da aplicação (mencionado acima), padrão 300x300 pixels;

**info**: detalhes do incidente e dos veículos e pessoas envolvidos

**placas**: um array contendo as placas dos veículos envolvidos

**quantidades**: um dicionário com as quantidades de cada tipo de veículo


O campo **info** contém os seguintes atributos:

**latitude**:

**longitude**: posição do centro do mapa no relatório

**zoom**: nível de zoom preferido para montagem do relatório

**paths**: um array contendo as linhas desenhadas no relatório - isto inclui as linhas de trajeto aparente, faixas de pedestre e linha central desenhadas

**vehicles**: este array contém não apenas os veículos, mas também os demais objetos presentes na cena: pedestres, obstáculos, indicadores de sentido na via, postes e árvores.



######Paths
Os traçados (produzidos com desenho a dedo) são estilizados de forma a representar diferentes marcações na cena do incidente bem como itens de infraestrutura.
Atualmente há quatro estilos de linha:
* marca de freada (1)
* linha central dupla (5)
* trajeto aparente (3)
* faixa de pedestre (2)

o estilo é definido em cada objeto JSON Path com o parâmetro **style**.
Além disto há o campo **geom**, contendo um array de objetos com latitude e longitude. E também o array **points**, que define pares de valores correspondentes à posição de cada ponto no nível de zoom definido para  o relatório. Este array serve para tornar mais rápida a renderização do traçado, sem necessidade de converter cada par de coordenadas para as coordenadas da tela.

######Vehicles
O array vehicles contém todos os objetos colocados no diagrama: veículos, pedestres, postes, árvores e obstáculos.
Cada objeto é definido com a propriedade model, que indica qual é o tipo de objeto:

*  AUTO = 0;
*  CAMINHAO = 1;
*  CAMINHONETE=2;
*  CAMIONETA=3;
*  CARROCA=4;
*  MICROONIBUS=5;
*  MOTO = 6;
*  ONIBUS = 7;
*  REBOQUE=8;
*  SEMI=9;
*  TAXI=10;
*  TRAILER=11;
*  VIATURA=12;
*  PEDESTRE = 13;
*  BICI = 14;
*  COLISAO = 15;
*  OBSTACULO = 16;
*  SENTIDO = 17;
*  ARVORE = 18;
*  SPU = 19;

Além deste parâmetro também há os valores de width e height, que definem o tamanho do objeto, em metros, para ser renderizado no mapa, e o heading, que indica o ângulo, em graus no sentido horário, em que o objeto é girado.
