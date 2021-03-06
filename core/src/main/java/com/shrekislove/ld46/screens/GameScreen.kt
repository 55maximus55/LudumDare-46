package com.shrekislove.ld46.screens

import box2dLight.RayHandler
import box2dLight.RayHandler.setGammaCorrection
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import com.shrekislove.ld46.Main
import com.shrekislove.ld46.ecs.systems.*
import com.shrekislove.ld46.ecs.systems.box2d.*
import com.shrekislove.ld46.ecs.systems.rayhandler.RayHandlerCarFlashLightUpdateSystem
import com.shrekislove.ld46.ecs.systems.rayhandler.RayHandlerPlayerFlashLightSystem
import com.shrekislove.ld46.ecs.systems.rayhandler.RayHandlerUpdateBodyPositionsSystem
import com.shrekislove.ld46.ecs.systems.rayhandler.RayHandlerUpdateLightPositionsSystem
import com.shrekislove.ld46.entities.Car
import com.shrekislove.ld46.entities.Footer
import com.shrekislove.ld46.entities.Player
import com.shrekislove.ld46.utils.Box2dContactListener
import com.shrekislove.ld46.utils.Box2dWallsFromTiledMapCreator
import com.shrekislove.ld46.utils.LightSpawner
import com.shrekislove.ld46.utils.ObjectFromTiledMapGetter
import ktx.vis.table

class GameScreen : LibScreen() {

    val camera = OrthographicCamera().apply {
        setToOrtho(false)

        position.x = 0f
        position.y = 0f
    }
    val fov = 256f

    val map = TmxMapLoader().load("maps/OLOLO.tmx")
    val PPM = 32f

    lateinit var world: World
    lateinit var lightWorld: World
    lateinit var rayHandler: RayHandler

    lateinit var task: VisLabel
    lateinit var moneyLabel: VisLabel
    lateinit var hungryLabel: VisLabel

    var isNewGame = true

    val hud = table {
        setFillParent(true)
        top()
        left()

        label("Task: ")
        task = label(text = "")
        row()
        moneyLabel = label(text = "")
        row()
        hungryLabel = label(text = "")
    }

    override fun show() {
        world = World(Vector2(), true).apply {
            setContactListener(Box2dContactListener(map, PPM, task))
        }
        lightWorld = World(Vector2(), true)
        rayHandler = RayHandler(lightWorld).apply {
            setAmbientLight(0.4f)
            setBlur(true)
            setBlurNum(1)
            setCulling(true)
            setGammaCorrection(true)
        }

        // systems
        ecsEngine.apply {
            addSystem(Box2dTopdownPlayerMovementSystem())
            addSystem(Box2dFootersControlSystem())
            addSystem(FooterUpdateSpriteSystem())
            addSystem(PlayerSpriteUpdateSystem())
            addSystem(Box2dTeleportSystem())

            addSystem(RayHandlerPlayerFlashLightSystem())
            addSystem(RayHandlerCarFlashLightUpdateSystem())

            addSystem(Box2dWorldStepSystem(world, 10, 10))

            addSystem(RayHandlerUpdateBodyPositionsSystem(PPM))
            addSystem(RayHandlerUpdateLightPositionsSystem())

            addSystem(Box2dTopdownUpdateSpritePositionsSystem(world, PPM))
            addSystem(UpdateCameraPositionSystem(camera, PPM))
            addSystem(RenderSystem(camera, map, world, lightWorld, rayHandler, PPM))

            addSystem(GameHudUpdateSystem(moneyLabel, hungryLabel))
            addSystem(CheckBomjFightSystem())
            addSystem(PizdecSystem(world, PPM, map))
            addSystem(PizdimVorogivSystem(PPM))

            addSystem(UpdateMusicSystem())
        }
        // entities
        ecsEngine.apply {
            val playerStartPos = ObjectFromTiledMapGetter().getPosition(map, "player")
            addEntity(Player().create(playerStartPos.cpy().scl(1f / PPM), world, lightWorld, rayHandler, PPM))
            camera.position.apply {
                x = playerStartPos.x
                y = playerStartPos.y
            }

            val footer1Positions = ObjectFromTiledMapGetter().getPositions(map, "footer1").apply {
                for (i in iterator()) {
                    i.scl(1f / PPM)
                }
            }
            addEntity(Footer().create(footer1Positions, world, lightWorld, rayHandler, PPM))

            val car1Positions = ObjectFromTiledMapGetter().getPositions(map, "car1").apply {
                for (i in iterator()) {
                    i.scl(1f / PPM)
                }
            }
            addEntity(Car().create(car1Positions, world, lightWorld, rayHandler, PPM))
            val car2Positions = ObjectFromTiledMapGetter().getPositions(map, "car2").apply {
                for (i in iterator()) {
                    i.scl(1f / PPM)
                }
            }
            addEntity(Car().create(car2Positions, world, lightWorld, rayHandler, PPM))
            val car3Positions = ObjectFromTiledMapGetter().getPositions(map, "car3").apply {
                for (i in iterator()) {
                    i.scl(1f / PPM)
                }
            }
            addEntity(Car().create(car3Positions, world, lightWorld, rayHandler, PPM))
            val car4Positions = ObjectFromTiledMapGetter().getPositions(map, "car4").apply {
                for (i in iterator()) {
                    i.scl(1f / PPM)
                }
            }
            addEntity(Car().create(car4Positions, world, lightWorld, rayHandler, PPM))
        }
        Box2dWallsFromTiledMapCreator().apply {
            createWalls(world, PPM, map, "walls")
            createWalls(lightWorld, 1f, map, "lightwalls")
            createTriggers(world, PPM, map)
        }
        LightSpawner().createLights(map, rayHandler, PPM)

        task.setText("You're hungry, go to check the fridge")
        com.shrekislove.ld46.clear()
        Main.context.inject<Stage>().addActor(hud)
    }

    override fun hide() {
        for (i in ecsEngine.systems) {
            ecsEngine.removeSystem(i)
        }

        world.dispose()
        lightWorld.dispose()
        rayHandler.dispose()
        hud.remove()

        UpdateMusicSystem.stop()
    }

    override fun resize(width: Int, height: Int) {
        camera.apply {
            viewportWidth = fov * Gdx.graphics.width.toFloat() / Gdx.graphics.height.toFloat()
            viewportHeight = fov
            update()
        }
        rayHandler.resizeFBO(width / 4, height / 4)
    }

}