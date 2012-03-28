<?php 

$a = new Imagick();
$a->readImage('../imagens/low'.str_pad($background, 2, '0').'.png');
$b = new Imagick();
$b->readImage("../imagens/".$nome_foto);
$a->compositeImage($b, Imagick::COMPOSITE_DEFAULT, 0, 0); 
$a->setImageFileName("../output/capa.jpg"); 
$a->writeImage(); 
