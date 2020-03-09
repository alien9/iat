import 'ol/ol.css';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import {OSM, TileDebug} from 'ol/source';
import TileWMS from 'ol/source/TileWMS';
import {transform} from 'ol/proj';
import {fromLonLat} from 'ol/proj';
import {addCoordinateTransforms} from 'ol/proj';
import {getPixelFromCoordinate} from 'ol/pixel';
var dict=require('data.js');
const fs=require('fs');
var data = JSON.parse(fs.readFileSync('data.json', 'utf-8'));

data=data.data[1];
console.log(data);
var map = new Map({
  layers: [
    new TileLayer({
      source: new OSM()
    }),
    new TileLayer({
       source: new TileWMS({
          url: 'http://cetsp1.cetsp.com.br:10084/geoserver/cetmdc/wms',
          params: {
            'LAYERS': 'cetmdc:mdcViario_lg',
            'TILED': true,
            'service': 'WMS',
            'version': '1.1.0',
            'request': 'GetMap',
            'layers': 'cetmdc:mdcViario_lg'
          },
          serverType: 'geoserver'
       })
    })
  ],
  target: 'map',
  view: new View({
    center: transform([data.info.longitude,data.info.latitude], 'EPSG:4326', 'EPSG:3857'),
    zoom: data.info.zoom
  })
});
var image_canvas=document.getElementById('image_canvas');
var image_ctx = image_canvas.getContext("2d");
var now_drawing=0;
var image = new Image();
image.onload = function() {
  image_ctx.drawImage(image, 0, 0);
};
image.src = "data:image/png;base64,"+data.image;
console.log(fromLonLat([-46,-23]));

var num_sprites=2;
var sprites=[];
/*
    public static final int AUTO = 0;
    public static final int CAMINHAO = 1;
    public static final int CAMINHONETE=2;
    public static final int CAMIONETA=3;
    public static final int CARROCA=4;
    public static final int MICROONIBUS=5;
    public static final int MOTO = 6;
    public static final int ONIBUS = 7;
    public static final int REBOQUE=8;
    public static final int SEMI=9;
    public static final int TAXI=10;
    public static final int TRAILER=11;
    public static final int VIATURA=12;
    public static final int PEDESTRE = 13;
    public static final int BICI = 14;
    public static final int COLISAO = 15;
    public static final int OBSTACULO = 16;
    public static final int SENTIDO = 17;
    public static final int ARVORE = 18;
    public static final int SPU = 19;*/




var images=[
    "http://localhost/images/carro_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/motorcycle_090.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/pessoa.png",
    "http://localhost/images/bici000.png",
    "http://localhost/images/explode.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png",
    "http://localhost/images/truck_000.png"
];
for (var i=0;i<images.length;i++){
    var img1=new Image();
    img1.onload=draw;
    img1.src=images[i];
    sprites.push(img1);
}
console.log("images loading..");
function draw(){
    now_drawing++;
    if(now_drawing < images.length+1) {
        console.log("falta coisa");
        return;
    }
    var e=map.getView().calculateExtent(map.getSize());
    var canvas=document.getElementById('canvas');
    var metersByPixels=Math.sqrt(Math.pow(e[2]-e[0],2)+Math.pow(e[3]-e[1],2)) / Math.sqrt(Math.pow(canvas.width,2)+Math.pow(canvas.height,2));
    var pixelsPerMeter=Math.sqrt(Math.pow(canvas.width,2)+Math.pow(canvas.height,2))/Math.sqrt(Math.pow(e[2]-e[0],2)+Math.pow(e[3]-e[1],2));
    var scale=1.0;
    var height=canvas.height;
    var ctx = canvas.getContext("2d");
    for(var i=0;i<data.info.paths.length;i++){
        var p=data.info.paths[i];
        console.log(p);
        switch(p.style){
            case 2: // faixa de pedestres
                ctx.strokeStyle='#dfdfd6';
                ctx.lineWidth=3*pixelsPerMeter;
                ctx.lineCap="butt";
                ctx.setLineDash([0.5*pixelsPerMeter,0.3 * pixelsPerMeter]);
            break;
            case 3: // trajeto tracejado
                ctx.strokeStyle='#000000';
                ctx.lineCap="butt";
                ctx.lineWidth=6;
                ctx.setLineDash([14, 6]);
            break;

        }
        ctx.beginPath();
        console.log("comeco");
        var tp=map.getPixelFromCoordinate(fromLonLat([p.geom[0].longitude,p.geom[0].latitude]));
        console.log(tp);

        ctx.moveTo(tp[0],tp[1]);
        for(var j=1; j < p.geom.length; j++){
            tp=map.getPixelFromCoordinate(fromLonLat([p.geom[j].longitude,p.geom[j].latitude]));

    //        ctx.quadraticCurveTo(p.points[j][0], p.points[j][1]-OT, p.points[j+1][0], p.points[j+1][1]-OT);
            ctx.lineTo(tp[0], tp[1]);
        }

        ctx.stroke();
    }
    for(var i=0;i<data.info.vehicles.length;i++){
        var v=data.info.vehicles[i];
        var c=map.getPixelFromCoordinate(fromLonLat([v.longitude,v.latitude]));
        console.log(v.width);
        console.log("a");
        //ctx.beginPath();
        var lw=v.width*pixelsPerMeter;
        var ll=v.length*pixelsPerMeter;
        //ctx.rect(c[0]-lw/2, c[1]-ll/2, lw, ll);
        //ctx.translate();
        //ctx.rotate(v.position.heading);
        //ctx.stroke();
        var img=sprites[v.model];
        ctx.save();
        ctx.translate(c[0], c[1]);
        ctx.rotate(v.position.heading*Math.PI/180);
        ctx.translate(-c[0], -c[1]);
        ctx.drawImage(img, c[0]-lw/2, c[1]-ll/2, lw, ll);
        ctx.restore();
    }
}
function scalePreserveAspectRatio(imgW,imgH,maxW,maxH){
  return(Math.min((maxW/imgW),(maxH/imgH)));
}


map.once('postrender', function(event) {
    draw();
});

console.log(map);