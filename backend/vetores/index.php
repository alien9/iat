<?php
$ficura='<image
     xlink:href="imagem-28.png"
     x="-96.716019"
     y="-96.112076"
     width="407.92758"
     height="607.04358"
     id="image9559" />';
$bg=file_get_contents('arte1.svg');

file_put_contents('comp.svg', str_replace('</svg>', $ficura.'</svg>', $bg));


echo "wtf";
