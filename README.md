IAT

# Investigação de Acidentes de Trânsito

Esta aplicação cria registros de incidentes de trânsito. O IAT pode ser acionado por uma outra aplicação no ambiente Android. Uma implementação possível é por meio do plugin *bigrs-croqui*, que é compatível com o ambiente Cordova.
Tanto o plugin quanto qualquer outra aplicação Android acionam o IAT por meio do Intent action br.com.cetsp.iat.DRAW.
A aplicação está preparada para ser receber alguns parâmetros e responder para o emissor com os dados produzidos, que incluem os veículos envolvidos, suas posições na cena e o croqui detalhando a ocorrência.

**Ambiente**

A versão atual foi compilada com o Android SDK 26. Foi utilizada a biblioteca nativa jsqlite com a extensão [Spatialite](https://www.gaia-gis.it/fossil/libspatialite/wiki?name=splite-android).


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
Os relatórios produzidos com a ferramenta IAT Android são retornados para a aplicação acionadora serializados - isto é, o formato é texto JSON. Cada registro consiste em um conjunto de informações que descrevem o incidente e permitem reconstituir a cena retratada no relatório. 

No primeiro nível da estrutura temos os parâmetros:

image: em formato PNG, com as dimensões especificadas na chamada da aplicação (mencionado acima), padrão 300x300 pixels;
info: detalhes do incidente e dos veículos e pessoas envolvidos
placas: um array contendo as placas dos veículos envolvidos
quantidades: um dicionário com as quantidades de cada tipo de veículo

