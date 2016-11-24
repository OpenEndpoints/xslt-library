# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.hostname = "offerready-xslt-library"
  config.vm.box = "bento/debian-7.9"
  
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "500"
  end
  
  # runs as root within the VM
  config.vm.provision "shell", inline: %q{
  
    set -e  # stop on error

    echo --- General OS installation
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get upgrade -q -y    # grub upgrade warnings mess with the terminal
    apt-get -q -y install vim ant subversion ntp unattended-upgrades 

    echo --- Install Java 8
    echo 'Acquire::http::Proxy { download.oracle.com DIRECT; };' >> /etc/apt/apt.conf.d/01proxy
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
    apt-get update
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
    apt-get -qy install oracle-java8-installer

    echo --- Build software
    ant -f /vagrant/build.xml
  }
  
  config.vm.provision "shell", run: "always", inline: %q{
  
    set -e  # stop on error
    
    echo ''
    echo '-----------------------------------------------------------------'
    echo 'After "vagrant ssh", use:'
    echo '  ant -f /vagrant/build.xml'
    echo '-----------------------------------------------------------------'
    echo ''
  }
  
end
