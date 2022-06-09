package chipyard.fpga.vc707

import sys.process._
import chipsalliance.rocketchip.config.Config
import freechips.rocketchip.diplomacy.DTSTimebase
import freechips.rocketchip.subsystem.ExtMem
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import sifive.fpgashells.shell.xilinx.VC7071GDDRSize
import testchipip.SerialTLKey
import sifive.fpgashells.shell.DesignKey
import chipyard.{ChipTop, DefaultClockFrequencyKey, SmallBoomConfig}
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink.BootROMLocated
import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(address = BigInt(0x64000000L), nTxEntries = 1024, nRxEntries = 1024))
  //case PeripherySPIKey => List(SPIParams(rAddress = BigInt(0x10023000)))
})

class WithSystemModifications extends Config((site, here, up) => {
  case DTSTimebase => BigInt(1000000)
  case BootROMLocated(x) => up(BootROMLocated(x), site).map { p =>
    // invoke makefile for uart boot
    val freqMHz = (50 * 1e6).toLong
    val make = s"make -C fpga/src/main/resources/vc707/uartboot PBUS_CLK=${freqMHz} bin"
    require (make.! == 0, "Failed to build bootrom")
    p.copy(hang = 0x10000, contentFileName = s"./fpga/src/main/resources/vc707/uartboot/build/bootrom.bin")
  }
  case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(size = site(VC7071GDDRSize)))) // set extmem to DDR size
  case SerialTLKey => None // remove serialized tl port
})

// DOC include start: AbstractVC707 and Rocket
class WithVC707Tweaks extends Config(
  // harness binders
    new WithUART ++
    new WithDDRMem ++
    //new WithSPISDCard ++
    // io binders
    new WithUARTIOPassthrough ++
    new WithTLIOPassthrough ++
    //new WithSPIIOPassthrough ++
    // other configuration
    new WithDefaultPeripherals ++
    new chipyard.config.WithTLBackingMemory ++ // use TL backing memory
    new WithSystemModifications ++ // setup busses, use sdboot bootrom, setup ext. mem. size
    new chipyard.config.WithNoDebug ++ // remove debug module
    new freechips.rocketchip.subsystem.WithoutTLMonitors ++
    new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
    new WithFPGAFrequency(50) // default 100MHz freq
)

class RocketVC707Config extends Config(
  new WithVC707Tweaks ++
    new chipyard.RocketConfig)

class BoomVC707Config extends Config(
    new WithVC707Tweaks ++
    new SmallBoomConfig)

class WithFPGAFrequency(fMHz: Double) extends Config(
  new chipyard.config.WithPeripheryBusFrequency(fMHz) ++ // assumes using PBUS as default freq.
    new chipyard.config.WithMemoryBusFrequency(fMHz)
)

class WithFPGAFreq25MHz extends WithFPGAFrequency(25)
class WithFPGAFreq50MHz extends WithFPGAFrequency(50)
class WithFPGAFreq75MHz extends WithFPGAFrequency(75)
class WithFPGAFreq100MHz extends WithFPGAFrequency(100)

