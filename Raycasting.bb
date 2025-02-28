; Initialisation du programme
Graphics3D 640,480,16,2
SetBuffer BackBuffer()

; Charger les textures
Global wallTexture = LoadTexture("wall.bmp")
Global floorTexture = LoadTexture("wall.bmp")
Global ceilTexture = LoadTexture("wall.bmp")
If wallTexture = 0 Or floorTexture = 0 Or ceilTexture = 0 Then RuntimeError "Texture non trouvée!"

; Constantes
Const MAP_WIDTH = 24
Const MAP_HEIGHT = 24
Const PLAYER_SIZE# = 0.2 ; Taille de la hitbox du joueur
Const STEP_CHECK# = 0.05 ; Pas pour vérifier le trajet
Const LIGHT_RANGE# = 10.0 ; Portée maximale de la lumière
Const MOUSE_SENSITIVITY# = 0.002 ; Sensibilité de la souris (ajustable)

; Carte simple (1 = mur, 0 = vide)
Dim map(MAP_WIDTH,MAP_HEIGHT)
For x = 0 To MAP_WIDTH-1
    For y = 0 To MAP_HEIGHT-1
        If x = 0 Or x = MAP_WIDTH-1 Or y = 0 Or y = MAP_HEIGHT-1 Then
            map(x,y) = 1
        Else
            map(x,y) = 0
        EndIf
    Next
Next
map(8,8) = 1
map(8,16) = 1
map(16,8) = 1

; Variables du joueur
Global posX# = 12
Global posY# = 12
Global dirX# = -1
Global dirY# = 0
Global planeX# = 0
Global planeY# = 0.66

; Vitesse
Global moveSpeed# = 0.1
Global rotSpeed# = 0.5

; Initialisation de la souris
HidePointer ; Cache le curseur
MoveMouse GraphicsWidth()/2, GraphicsHeight()/2 ; Centre la souris

; Fonction pour vérifier les collisions
Function CheckCollision#(x#, y#)
    If x < 0 Or x >= MAP_WIDTH Or y < 0 Or y >= MAP_HEIGHT Then Return True
    If map(Int(x - PLAYER_SIZE), Int(y - PLAYER_SIZE)) > 0 Then Return True
    If map(Int(x + PLAYER_SIZE), Int(y - PLAYER_SIZE)) > 0 Then Return True
    If map(Int(x - PLAYER_SIZE), Int(y + PLAYER_SIZE)) > 0 Then Return True
    If map(Int(x + PLAYER_SIZE), Int(y + PLAYER_SIZE)) > 0 Then Return True
    Return False
End Function

; Fonction pour déplacer avec gestion des collisions
Function MovePlayer(dx#, dy#)
    steps = Int(moveSpeed / STEP_CHECK) + 1
    stepX# = dx / steps
    stepY# = dy / steps
    
    For i = 1 To steps
        newX# = posX + stepX
        newY# = posY + stepY
        
        If Not CheckCollision(newX, posY) Then posX = newX Else Exit
        If Not CheckCollision(posX, newY) Then posY = newY Else Exit
    Next
End Function

; Fonction pour ajuster la luminosité d'une couleur
Function AdjustLight(color, distance#)
    brightness# = 1.0 - (distance / LIGHT_RANGE)
    If brightness < 0 Then brightness = 0
    
    r = (color Shr 16) And $FF
    g = (color Shr 8) And $FF
    b = color And $FF
    
    r = Int(r * brightness)
    g = Int(g * brightness)
    b = Int(b * brightness)
    
    Return (r Shl 16) Or (g Shl 8) Or b
End Function

; Boucle principale
While Not KeyHit(1)
    
    ; Contrôles déplacement avec le clavier
    If KeyDown(200) Then MovePlayer(dirX * moveSpeed, dirY * moveSpeed) ; Avancer
    If KeyDown(208) Then MovePlayer(-dirX * moveSpeed, -dirY * moveSpeed) ; Reculer
    
    ; Rotation avec les touches (optionnel, conservé)
    If KeyDown(203) Then ; Tourner à gauche
        oldDirX# = dirX
        dirX = dirX * Cos(rotSpeed) - dirY * Sin(rotSpeed)
        dirY = oldDirX * Sin(rotSpeed) + dirY * Cos(rotSpeed)
        oldPlaneX# = planeX
        planeX = planeX * Cos(rotSpeed) - planeY * Sin(rotSpeed)
        planeY = oldPlaneX * Sin(rotSpeed) + planeY * Cos(rotSpeed)
    EndIf
    
    If KeyDown(205) Then ; Tourner à droite
        oldDirX# = dirX
        dirX = dirX * Cos(-rotSpeed) - dirY * Sin(-rotSpeed)
        dirY = oldDirX * Sin(-rotSpeed) + dirY * Cos(-rotSpeed)
        oldPlaneX# = planeX
        planeX = planeX * Cos(-rotSpeed) - planeY * Sin(-rotSpeed)
        planeY = oldPlaneX * Sin(-rotSpeed) + planeY * Cos(-rotSpeed)
    EndIf
    
    ; Rotation avec la souris
    mouseMoveX = MouseXSpeed() * 30 ; Déplacement horizontal de la souris
    If mouseMoveX <> 0 Then
        rotAngle# = -mouseMoveX * MOUSE_SENSITIVITY ; Angle de rotation basé sur la souris
        oldDirX# = dirX
        dirX = dirX * Cos(rotAngle) - dirY * Sin(rotAngle)
        dirY = oldDirX * Sin(rotAngle) + dirY * Cos(rotAngle)
        oldPlaneX# = planeX
        planeX = planeX * Cos(rotAngle) - planeY * Sin(rotAngle)
        planeY = oldPlaneX * Sin(rotAngle) + planeY * Cos(rotAngle)
    EndIf
    MoveMouse GraphicsWidth()/2, GraphicsHeight()/2 ; Recentre la souris
    
    ; Verrouillage des buffers
    LockBuffer BackBuffer()
    LockBuffer TextureBuffer(wallTexture)
    LockBuffer TextureBuffer(floorTexture)
    LockBuffer TextureBuffer(ceilTexture)
    
    ; Raycasting
    For x = 0 To 639
        cameraX# = 2 * x / Float(640) - 1
        rayDirX# = dirX + planeX * cameraX
        rayDirY# = dirY + planeY * cameraX
        
        mapX = Int(posX)
        mapY = Int(posY)
        
        If rayDirX = 0 Then rayDirX = 0.0001
        If rayDirY = 0 Then rayDirY = 0.0001
        
        deltaDistX# = Abs(1/rayDirX)
        deltaDistY# = Abs(1/rayDirY)
        
        hit = 0
        side = 0
        
        If rayDirX < 0 Then
            stepX = -1
            sideDistX# = (posX - mapX) * deltaDistX
        Else
            stepX = 1
            sideDistX# = (mapX + 1.0 - posX) * deltaDistX
        EndIf
        
        If rayDirY < 0 Then
            stepY = -1
            sideDistY# = (posY - mapY) * deltaDistY
        Else
            stepY = 1
            sideDistY# = (mapY + 1.0 - posY) * deltaDistY
        EndIf
        
        While hit = 0
            If sideDistX < sideDistY Then
                sideDistX = sideDistX + deltaDistX
                mapX = mapX + stepX
                side = 0
            Else
                sideDistY = sideDistY + deltaDistY
                mapY = mapY + stepY
                side = 1
            EndIf
            
            If map(mapX,mapY) > 0 Then hit = 1
        Wend
        
        If side = 0 Then
            perpWallDist# = (mapX - posX + (1 - stepX) / 2) / rayDirX
        Else
            perpWallDist# = (mapY - posY + (1 - stepY) / 2) / rayDirY
        EndIf
        
        ; Correction de l'effet fish-eye
        ; On multiplie par le cosinus de l'angle entre dir et rayDir
        cosAngle# = (dirX * rayDirX + dirY * rayDirY) / (Sqr(dirX * dirX + dirY * dirY) * Sqr(rayDirX * rayDirX + rayDirY * rayDirY))
        If cosAngle = 0 Then cosAngle = 0.0001 ; Protection contre division par zéro
        perpWallDist = perpWallDist * cosAngle ; Distance corrigée
        
        If perpWallDist < 0.1 Then perpWallDist = 0.1
        
        lineHeight = Int(480 / perpWallDist)
        drawStart = -lineHeight / 2 + 480 / 2
        If drawStart < 0 Then drawStart = 0
        drawEnd = lineHeight / 2 + 480 / 2
        If drawEnd >= 480 Then drawEnd = 479
        
        ; Calcul des coordonnées de texture pour le mur
        If side = 0 Then
            wallX# = posY + perpWallDist * rayDirY
        Else
            wallX# = posX + perpWallDist * rayDirX
        EndIf
        wallX = wallX - Floor(wallX)
        
        texX = Int(wallX * Float(TextureWidth(wallTexture)))
        If side = 0 And rayDirX > 0 Then texX = TextureWidth(wallTexture) - texX - 1
        If side = 1 And rayDirY < 0 Then texX = TextureWidth(wallTexture) - texX - 1
        
        ; Rendu du plafond
        For y = 0 To drawStart - 1
            currentDist# = 480.0 / (480.0 - 2.0 * y)
            If currentDist < 0.1 Then currentDist = 0.1
            
            weight# = currentDist / perpWallDist
            currentCeilX# = weight * (posX + rayDirX * perpWallDist) + (1.0 - weight) * posX
            currentCeilY# = weight * (posY + rayDirY * perpWallDist) + (1.0 - weight) * posY
            
            ceilTexX = Int(currentCeilX * TextureWidth(ceilTexture)) Mod TextureWidth(ceilTexture)
            ceilTexY = Int(currentCeilY * TextureHeight(ceilTexture)) Mod TextureHeight(ceilTexture)
            
            ceilColor = ReadPixelFast(ceilTexX, ceilTexY, TextureBuffer(ceilTexture)) And $FFFFFF
            ceilColor = AdjustLight(ceilColor, currentDist)
            WritePixelFast x, y, ceilColor, BackBuffer()
        Next
        
        ; Rendu du sol
        For y = drawEnd + 1 To 479
            currentDist# = 480.0 / (2.0 * y - 480.0)
            If currentDist < 0.1 Then currentDist = 0.1
            
            weight# = currentDist / perpWallDist
            currentFloorX# = weight * (posX + rayDirX * perpWallDist) + (1.0 - weight) * posX
            currentFloorY# = weight * (posY + rayDirY * perpWallDist) + (1.0 - weight) * posY
            
            floorTexX = Int(currentFloorX * TextureWidth(floorTexture)) Mod TextureWidth(floorTexture)
            floorTexY = Int(currentFloorY * TextureHeight(floorTexture)) Mod TextureHeight(floorTexture)
            
            floorColor = ReadPixelFast(floorTexX, floorTexY, TextureBuffer(floorTexture)) And $FFFFFF
            floorColor = AdjustLight(floorColor, currentDist)
            WritePixelFast x, y, floorColor, BackBuffer()
        Next
        
        ; Rendu des murs
        For y = drawStart To drawEnd
            texY = ((y * 2 - 480 + lineHeight) * TextureHeight(wallTexture)) / (lineHeight * 2)
            pixelColor = ReadPixelFast(texX, texY, TextureBuffer(wallTexture)) And $FFFFFF
            
            If side = 1 Then
                pixelColor = (pixelColor Shr 1) And $7F7F7F
            EndIf
            pixelColor = AdjustLight(pixelColor, perpWallDist)
            WritePixelFast x, y, pixelColor, BackBuffer()
        Next
    Next
    
    ; Déverrouillage des buffers
    UnlockBuffer TextureBuffer(ceilTexture)
    UnlockBuffer TextureBuffer(floorTexture)
    UnlockBuffer TextureBuffer(wallTexture)
    UnlockBuffer BackBuffer()
    
    Flip
    Cls
Wend

End
