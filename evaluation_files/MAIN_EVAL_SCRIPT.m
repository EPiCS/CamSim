clear all
%run through all folder
start = ['./'];

allPossible = true;

scenarios = dir(start);

%OPTIONS FOR PLOTTING
drawHistos = false;
loadSummaryOnly = true; %if existing
plotCameraResults = false;
fileext = 'fig'; %svg or pdf or png for OCTAVE!!

for sc = 3:numel(scenarios)

    nok = false;
    if(strfind(scenarios(sc).name, '100'))
        nok = true;
    end
    if(strfind(scenarios(sc).name, 'figure'))
        nok = true;
    end
    
    AllScenRes = zeros(0,0);
    AllScenNames = zeros(0,0);
    HomoRunRes = zeros(0,0);
    AllStepRes = zeros(0,0);
    runs = 1;
    totalComm = zeros(0,0);
    if(~nok)
        if scenarios(sc).isdir
            scenname = scenarios(sc).name;
            res = strcmpi(scenarios(sc).name, 'figures')
            clf
            if res == 0 %scenario is not the figures directory
                scen = scenarios(sc).name;
                scen = strcat(scen, '/');
                
                %run through all subfolder 
                subs = dir(scen);
                
                homoNames = zeros(0,0);
                homoDataUtil = zeros(0,0);
                homoDataComm = zeros(0,0);
                banditsFound = false;
                %AllBanditInfo = zeros(0,0);
                for sfc = 3:numel(subs)
                    if subs(sfc).isdir
                        strcat(scenname, ' - ', subs(sfc).name)
                        
                        if numel(strfind(subs(sfc).name, 'ex')) > 0
                        	[AllBanditInfo, AllBanditNames]= LoadBanditSolverFiles(strcat(scen, subs(sfc).name), false);
                            banditsFound = true;
                        else
                            if(ContainsSummaryFile(strcat(scen, subs(sfc).name)) && loadSummaryOnly)
                                currentFile = strcat(scen, subs(sfc).name, '/summary.csv');
                                Mintermed = dlmread(currentFile, ',', 1, 0);
                                mComm = mean(Mintermed(:,6) / Mintermed(1,2));
                                mUtil = mean(Mintermed(:,4) / Mintermed(1,2));
                                AllScenNames = [AllScenNames; subs(sfc).name];
                                AllScenRes = [AllScenRes; mComm 0 mUtil 0 0 0 0];
                            else
                                %AllBanditInfo = zeros(0,0);
                                [Mfull, MCommStep, MUtilStep, MVisStep] = LoadAllFiles(strcat(scen, subs(sfc).name));
                                AllScenNames = [AllScenNames; subs(sfc).name];
                                [t1, t2] = CalculateStatsPerStep(MCommStep);
                                [t3, t4] = CalculateStatsPerStep(MUtilStep);
                                [t5, t6] = CalculateStatsPerStep(MVisStep);
                                [r1, r2] = CalculateStatsPerRun(MCommStep);
                                [r3, r4] = CalculateStatsPerRun(MUtilStep);
                                [r5, r6] = CalculateStatsPerRun(MVisStep);

                                if drawHistos
                                    if ~isempty(MCommStep)
                                       x = 0:1:max(MCommStep(:,1));
                                       hist(MCommStep(:,1),x);
                                       set(gca, 'XTick', x);
                                       axis([-.5, max(MCommStep(:,1))+1, -Inf, Inf]);
                                       title(scenname);
                                       saveas(gcf, strcat('figures/histos/hist', scenname(1:numel(scenname)), '_', subs(sfc).name, '.', fileext), fileext);
                                       saveas(gcf, strcat('figures/histos/hist', scenname(1:numel(scenname)), '_', subs(sfc).name, '.png'), 'png');
                                    end
                                end
                                
                                isHomo = IsHomogeneous(subs(sfc).name);
                                if(isHomo)
                                    homoNames = [homoNames; subs(sfc).name];
                                    homoDataUtil = [homoDataUtil t3];
                                    homoDataComm = [homoDataComm t1];
                                    AllStepRes = [AllStepRes; r1 r2 r3 r4 r5 r6];
                                end

                                if(runs == 1)
                                    if ~isempty(MCommStep)
                                        runs = numel(MCommStep(1,:));
                                    end
                                end

                                AllScenRes = [AllScenRes; mean(t1) mean(t2) mean(t3) mean(t4) mean(t5) mean(t6)];
                            end
                        end
                    end
                end
                normFactors = CalcNormFactors(AllScenRes);
               % painterrorplot(allscenres, allscennames, true, scen, true);
                % painthomos(allscenres, allscennames, true, strcat('errhomos', scen), true, true, fileext);
                 
                if plotCameraResults
                    findAndPlotCameraResults(scenname, normFactors);
                end

                if numel(AllScenNames) > 0
                   %figure
                    paintDistPlot(AllScenRes, AllScenNames, normFactors, scen, false, true, fileext);
                    paintHomos(AllScenRes, AllScenNames, normFactors, scen, true, false, fileext);
                end
                if banditsFound 
                    %figure
                    hetPaint = true
                    if numel(AllScenRes(:,1)) == 6
                        hetPaint = false;
                    end
                    paintBanditInfo(AllBanditInfo, AllBanditNames, scen, normFactors, true, hetPaint, fileext)
                end
            end
        end
    end

end

fine = '####################################### DONE #######################################'

